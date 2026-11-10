"""Synthetic SQLite-only UP70-B preload experiment; never opens a user's database.

Numbers describe this Python/SQLite model, not Room, Kotlin heap or Android TV performance.
The candidate streams rows with a hard entry cap and only prunes reached channels after a complete feed.
"""
from array import array
import argparse
import gc
import json
from pathlib import Path
import sqlite3
import statistics
import time
import tracemalloc

LAZY_SQL = "SELECT id, epgChannelId, startMs, contentHash FROM epg_programmes WHERE sourceId = ? AND epgChannelId = ? LIMIT ?"
ORDERED_SQL = "SELECT epgChannelId, startMs, id, contentHash FROM epg_programmes WHERE sourceId = ? ORDER BY epgChannelId, startMs"

class Snapshot:
    def __init__(self, cap):
        self.cap = cap
        self.channels = {}
        self.untracked = set()
        self.reached = set()
        self.entries = 0
        self.read = 0
        self.queries = 0

    def stale_ids(self, complete):
        if not complete:
            return []
        return [row_id for key in self.reached for row_id in self.channels.get(key, ((),(),()))[1]]

    def preload(self, rows):
        key = None
        current = None
        def flush():
            if current is not None:
                self.channels[key] = current
                self.entries += len(current[0])
        for next_key, start, row_id, content_hash in rows:
            self.read += 1
            if next_key != key:
                flush()
                if self.entries == self.cap:
                    return
                key = next_key
                current = (array("q"), array("q"), array("i"))
            if self.entries + len(current[0]) == self.cap:
                # Do not promote a partial channel or use its prefix to delete anything.
                self.untracked.add(key)
                return
            current[0].append(start)
            current[1].append(row_id)
            current[2].append(content_hash)
        flush()

    def reach(self, db, key):
        self.reached.add(key)
        if key in self.channels or key in self.untracked:
            return
        remaining = self.cap - self.entries
        if remaining <= 0:
            self.untracked.add(key)
            return
        self.queries += 1
        rows = db.execute(LAZY_SQL, (1,key,remaining + 1)).fetchall()
        self.read += len(rows)
        if len(rows) > remaining:
            self.untracked.add(key)
            return
        self.channels[key] = (array("q",(row[2] for row in rows)), array("q",(row[0] for row in rows)), array("i",(row[3] for row in rows)))
        self.entries += len(rows)


def measure(db, feed, cap, preload):
    gc.collect()
    tracemalloc.start()
    start = time.perf_counter_ns()
    state = Snapshot(cap)
    if preload and cap > 0:
        state.queries += 1
        cursor = db.execute(ORDERED_SQL,(1,))
        try:
            state.preload(cursor)
        finally:
            cursor.close()
    startup_ns = time.perf_counter_ns() - start
    for key in feed:
        state.reach(db,key)
    elapsed = time.perf_counter_ns() - start
    _, peak = tracemalloc.get_traced_memory()
    tracemalloc.stop()
    assert state.entries <= cap
    return dict(totalMs=elapsed/1e6, preloadBeforeFeedMs=startup_ns/1e6, queries=state.queries,
                rowsRead=state.read, retainedEntries=state.entries, pythonPeakBytes=peak)


def run(repeats):
    results = []
    for label,channels,programmes,feed_count in [("full_240",240,400,240),("partial_12_of_240",240,400,12),("full_3000_small_channels",3000,16,3000)]:
        with sqlite3.connect(":memory:") as db:
            db.executescript("CREATE TABLE epg_programmes(id INTEGER PRIMARY KEY,sourceId INTEGER,epgChannelId TEXT,startMs INTEGER,contentHash INTEGER); CREATE UNIQUE INDEX index_epg_programmes_natural_key ON epg_programmes(sourceId,epgChannelId,startMs);")
            keys = [f"c{i:05d}" for i in range(channels)]
            db.executemany("INSERT INTO epg_programmes VALUES (?,?,?,?,?)", ((i*programmes+j+1,1,key,j*3600000,j) for i,key in enumerate(keys) for j in range(programmes)))
            db.commit()
            feed = keys[:feed_count]
            item = dict(case=label,storedRows=channels*programmes,feedChannels=feed_count,cap=100000)
            for preload,mode in [(False,"lazy"),(True,"orderedPreload")]:
                samples = [measure(db,feed,100000,preload) for _ in range(repeats)]
                item[mode] = {key:statistics.median(sample[key] for sample in samples) for key in samples[0]}
            item["queryPlan"] = [row[3] for row in db.execute("EXPLAIN QUERY PLAN "+ORDERED_SQL,(1,))]
            results.append(item)
    return dict(sqliteVersion=sqlite3.sqlite_version,repeats=repeats,scope="Python/SQLite synthetic model, not Android/Room benchmark",cases=results)

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--output",type=Path,required=True)
    parser.add_argument("--repeats",type=int,default=3)
    args = parser.parse_args()
    if args.repeats < 1: parser.error("repeats must be positive")
    result = run(args.repeats)
    args.output.write_text(json.dumps(result,indent=2),encoding="utf-8")
    print(json.dumps(result,indent=2))
