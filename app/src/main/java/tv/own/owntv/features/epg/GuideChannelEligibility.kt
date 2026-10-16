package tv.own.owntv.features.epg

import tv.own.owntv.core.customize.CustomizeKeys
import tv.own.owntv.core.database.entity.ChannelEntity

/** Matches can add candidates, never grant visibility withheld by the profile/source policy. */
internal fun guideChannelEligible(channel: ChannelEntity, sourceIds: Set<Long>,
    hiddenCategoryIds: Set<Long>, hiddenItems: Set<String>): Boolean =
    channel.sourceId in sourceIds && channel.categoryId !in hiddenCategoryIds &&
        CustomizeKeys.channel(channel) !in hiddenItems
