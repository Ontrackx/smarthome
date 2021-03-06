/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.core.thing.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.builder.BridgeBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.GenericThingBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.thing.dto.ChannelDTO;
import org.eclipse.smarthome.core.thing.dto.ChannelDTOMapper;
import org.eclipse.smarthome.core.thing.dto.ThingDTO;
import org.eclipse.smarthome.core.thing.internal.BridgeImpl;
import org.eclipse.smarthome.core.thing.internal.ThingImpl;

import com.google.common.base.Joiner;

/**
 * {@link ThingHelper} provides a utility method to create and bind items.
 *
 * @author Oliver Libutzki - Initial contribution
 * @author Andre Fuechsel - graceful creation of items and links
 * @author Benedikt Niehues - Fix ESH Bug 450236
 *         https://bugs.eclipse.org/bugs/show_bug.cgi?id=450236 - Considering
 *         ThingTypeDescription
 * @author Dennis Nobel - Removed createAndBindItems method
 * @author Kai Kreuzer - Added merge method
 */
public class ThingHelper {

    /**
     * Indicates whether two {@link Thing}s are technical equal.
     *
     * @param a
     *            Thing object
     * @param b
     *            another Thing object
     * @return true whether a and b are equal, otherwise false
     */
    public static boolean equals(Thing a, Thing b) {
        if (!a.getUID().equals(b.getUID())) {
            return false;
        }
        if (a.getBridgeUID() == null && b.getBridgeUID() != null) {
            return false;
        }
        if (a.getBridgeUID() != null && !a.getBridgeUID().equals(b.getBridgeUID())) {
            return false;
        }
        // configuration
        if (a.getConfiguration() == null && b.getConfiguration() != null) {
            return false;
        }
        if (a.getConfiguration() != null && !a.getConfiguration().equals(b.getConfiguration())) {
            return false;
        }
        // channels
        List<Channel> channelsOfA = a.getChannels();
        List<Channel> channelsOfB = b.getChannels();
        if (channelsOfA.size() != channelsOfB.size()) {
            return false;
        }
        if (!toString(channelsOfA).equals(toString(channelsOfB))) {
            return false;
        }
        return true;
    }

    private static String toString(List<Channel> channels) {
        List<String> strings = new ArrayList<>(channels.size());
        for (Channel channel : channels) {
            strings.add(channel.getUID().toString() + '#' + channel.getAcceptedItemType());
        }
        Collections.sort(strings);
        return Joiner.on(',').join(strings);
    }

    public static void addChannelsToThing(Thing thing, Collection<Channel> channels) {
        ((ThingImpl) thing).getChannelsMutable().addAll(channels);
    }

    /**
     * Merges the content of a ThingDTO with an existing Thing.
     * Where ever the DTO has null values, the content of the original Thing is kept.
     * Where ever the DTO has non-null values, these are used.
     * In consequence, care must be taken when the content of a list (like configuration, properties or channels) is to
     * be updated - the DTO must contain the full list, otherwise entries will be deleted.
     *
     * @param thing the Thing instance to merge the new content into
     * @param updatedContents a DTO which carries the updated content
     *
     * @return A Thing instance, which is the result of the merge
     */
    public static Thing merge(Thing thing, ThingDTO updatedContents) {

        GenericThingBuilder<?> builder;

        if (thing instanceof Bridge) {
            builder = BridgeBuilder.create(thing.getThingTypeUID(), thing.getUID());
        } else {
            builder = ThingBuilder.create(thing.getThingTypeUID(), thing.getUID());
        }

        // Update the label
        if (updatedContents.label != null) {
            builder.withLabel(updatedContents.label);
        } else {
            builder.withLabel(thing.getLabel());
        }

        // update bridge UID
        if (updatedContents.bridgeUID != null) {
            builder.withBridge(new ThingUID(updatedContents.bridgeUID));
        } else {
            builder.withBridge(thing.getBridgeUID());
        }

        // update thing configuration
        if (updatedContents.configuration != null && !updatedContents.configuration.keySet().isEmpty()) {
            builder.withConfiguration(new Configuration(updatedContents.configuration));
        } else {
            builder.withConfiguration(thing.getConfiguration());
        }

        // update thing properties
        if (updatedContents.properties != null) {
            builder.withProperties(updatedContents.properties);
        } else {
            builder.withProperties(thing.getProperties());
        }

        // Update the channels
        if (updatedContents.channels != null) {
            for (ChannelDTO channelDTO : updatedContents.channels) {
                builder.withChannel(ChannelDTOMapper.map(channelDTO));
            }
        } else {
            builder.withChannels(thing.getChannels());
        }

        Thing mergedThing = builder.build();

        // keep all child things in place on a merged bridge
        if (mergedThing instanceof BridgeImpl && thing instanceof Bridge) {
            Bridge bridge = (Bridge) thing;
            BridgeImpl mergedBridge = (BridgeImpl) mergedThing;
            for (Thing child : bridge.getThings()) {
                mergedBridge.addThing(child);
            }
        }

        return mergedThing;
    }
}
