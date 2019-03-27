/*******************************************************************************
 * Copyright (c) 2016, 2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.hono.adapter.lora.providers;

import org.eclipse.hono.adapter.lora.LoraMessageType;
import org.junit.Assert;
import org.junit.Test;

import io.vertx.core.json.JsonObject;

/**
 * Verifies behavior of {@link ProximusProvider}.
 */
public class ProximusProviderTest {

    private final ProximusProvider provider = new ProximusProvider();

    /**
     * Verifies that the extraction of the device id from a message is successful.
     */
    @Test
    public void extractDeviceIdFromLoraMessage() {
        final JsonObject loraMessage = LoraTestUtil.loadTestFile("proximus.uplink");
        final String deviceId = this.provider.extractDeviceId(loraMessage);

        Assert.assertEquals("56FDB2B433873F4C", deviceId);
    }

    /**
     * Verifies the extraction of a payload from a message is successful.
     */
    @Test
    public void extractPayloadFromLoraMessage() {
        final JsonObject loraMessage = LoraTestUtil.loadTestFile("proximus.uplink");
        final String payload = this.provider.extractPayloadEncodedInBase64(loraMessage);

        Assert.assertEquals("2205630000328c", payload);
    }

    /**
     * Verifies that the extracted message type matches uplink.
     */
    @Test
    public void extractTypeFromLoraUplinkMessage() {
        final JsonObject loraMessage = LoraTestUtil.loadTestFile("proximus.uplink");
        final LoraMessageType type = this.provider.extractMessageType(loraMessage);
        Assert.assertEquals(LoraMessageType.UPLINK, type);
    }
}
