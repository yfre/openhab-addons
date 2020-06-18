/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.io.homekit.internal.accessories;

import static org.openhab.io.homekit.internal.HomekitCharacteristicType.CURRENT_SLAT_STATE;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.openhab.io.homekit.internal.HomekitAccessoryUpdater;
import org.openhab.io.homekit.internal.HomekitSettings;
import org.openhab.io.homekit.internal.HomekitTaggedItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.hapjava.accessories.SlatAccessory;
import io.github.hapjava.characteristics.HomekitCharacteristicChangeCallback;
import io.github.hapjava.characteristics.impl.slat.CurrentSlatStateEnum;
import io.github.hapjava.characteristics.impl.slat.SlatTypeEnum;
import io.github.hapjava.services.impl.SlatService;

/**
 *
 * @author Eugen Freiter - Initial contribution
 */
public class HomekitSlatImpl extends AbstractHomekitAccessoryImpl implements SlatAccessory {
    private final Logger logger = LoggerFactory.getLogger(HomekitSlatImpl.class);
    private static final String CONFIG_SLAT_TYPE = "slatType";
    private final SlatTypeEnum slatType;

    private static final Map<String, SlatTypeEnum> CONFIG_SLAT_TYPE_MAPPING = new HashMap<String, SlatTypeEnum>() {
        {
            put("HORIZONTAL", SlatTypeEnum.HORIZONTAL);
            put("VERTICAL", SlatTypeEnum.VERTICAL);
        }
    };

    private final Map<CurrentSlatStateEnum, String> slatStateMapping = new EnumMap(CurrentSlatStateEnum.class) {
        {
            put(CurrentSlatStateEnum.FIXED, "FIXED");
            put(CurrentSlatStateEnum.JAMMED, "JAMMED");
            put(CurrentSlatStateEnum.SWINGING, "SWINGING");
        }
    };

    public HomekitSlatImpl(HomekitTaggedItem taggedItem, List<HomekitTaggedItem> mandatoryCharacteristics,
            HomekitAccessoryUpdater updater, HomekitSettings settings) throws IncompleteAccessoryException {
        super(taggedItem, mandatoryCharacteristics, updater, settings);
        final String slatTypeConfig = getAccessoryConfiguration(CONFIG_SLAT_TYPE, "HORIZONTAL");
        SlatTypeEnum type = CONFIG_SLAT_TYPE_MAPPING.get(slatTypeConfig.toUpperCase());
        slatType = type != null ? type : SlatTypeEnum.HORIZONTAL;
        getServices().add(new SlatService(this));
    }

    @Override
    public CompletableFuture<CurrentSlatStateEnum> getSlatState() {
        return CompletableFuture
                .completedFuture(getKeyFromMapping(CURRENT_SLAT_STATE, slatStateMapping, CurrentSlatStateEnum.FIXED));
    }

    @Override
    public void subscribeSlatState(final HomekitCharacteristicChangeCallback callback) {
        subscribe(CURRENT_SLAT_STATE, callback);
    }

    @Override
    public void unsubscribeSlatState() {
        unsubscribe(CURRENT_SLAT_STATE);
    }

    @Override
    public CompletableFuture<SlatTypeEnum> getSlatType() {
        return CompletableFuture.completedFuture(slatType);
    }
}
