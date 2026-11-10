import unittest
from benchmark_up70_b import Snapshot

class PreloadSafetyTest(unittest.TestCase):
    def test_oversized_channel_is_never_partially_promoted(self):
        state = Snapshot(2)
        state.preload(iter([("a",1,1,1),("a",2,2,2),("a",3,3,3)]))
        self.assertEqual(0,state.entries)
        self.assertEqual({},state.channels)
        self.assertEqual({"a"},state.untracked)
        self.assertEqual([],state.stale_ids(True))
    def test_exact_cap_preserves_only_complete_channels(self):
        state = Snapshot(2)
        state.preload(iter([("a",1,1,1),("a",2,2,2),("b",1,3,3)]))
        self.assertEqual(2,state.entries)
        self.assertEqual({"a"},set(state.channels))
        self.assertEqual(3,state.read)
    def test_unreached_or_incomplete_feed_never_prunes_preloaded_rows(self):
        state = Snapshot(3)
        state.preload(iter([("a",1,1,1),("b",1,2,2)]))
        self.assertEqual([],state.stale_ids(True))
        state.reached.add("a")
        self.assertEqual([1],state.stale_ids(True))
        self.assertEqual([],state.stale_ids(False))
    def test_zero_and_negative_timestamps_are_not_sentinels(self):
        state = Snapshot(3)
        state.preload(iter([("a",-1,1,1),("a",0,2,2)]))
        self.assertEqual([-1,0],list(state.channels["a"][0]))
    def test_zero_cap_does_not_keep_a_partial_first_channel(self):
        state = Snapshot(0)
        state.preload(iter([("a",0,1,1)]))
        self.assertEqual(0,state.entries)
        self.assertEqual({},state.channels)

if __name__ == "__main__": unittest.main()
