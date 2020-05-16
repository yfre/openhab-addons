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

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.openhab.io.homekit.internal.HomekitAccessoryUpdater;
import org.openhab.io.homekit.internal.HomekitCharacteristicType;
import org.openhab.io.homekit.internal.HomekitSettings;
import org.openhab.io.homekit.internal.HomekitTaggedItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.hapjava.accessories.AirQualityAccessory;
import io.github.hapjava.characteristics.HomekitCharacteristicChangeCallback;
import io.github.hapjava.characteristics.impl.airquality.AirQualityEnum;
import io.github.hapjava.services.impl.AirQualityService;

/**
 *
 * @author Eugen Freiter - Initial contribution
 *
 */
public class HomekitAirQualitySensorImpl extends AbstractHomekitAccessoryImpl implements AirQualityAccessory {
    private Logger logger = LoggerFactory.getLogger(HomekitAirQualitySensorImpl.class);
    private HashMap<String, AirQualityEnum> mapping = new HashMap<>();

    public HomekitAirQualitySensorImpl(HomekitTaggedItem taggedItem, List<HomekitTaggedItem> mandatoryCharacteristics,
            HomekitAccessoryUpdater updater, HomekitSettings settings) throws IncompleteAccessoryException {
        super(taggedItem, mandatoryCharacteristics, updater, settings);
        getServices().add(new AirQualityService(this));
    }

    private HashMap<String, AirQualityEnum> getMapping() {
        if (mapping.isEmpty()) {
            final HomekitSettings settings = getSettings();

            mapping.put(getAccessoryConfiguration("UNKNOWN", settings.airQualityUnknown).toUpperCase(),
                        AirQualityEnum.UNKNOWN);
            mapping.put(getAccessoryConfiguration("EXCELLENT", settings.airQualityExcellent).toUpperCase(),
                        AirQualityEnum.EXCELLENT);
            mapping.put(getAccessoryConfiguration("GOOD", settings.airQualityGood).toUpperCase(), AirQualityEnum.GOOD);
            mapping.put(getAccessoryConfiguration("FAIR", settings.airQualityFair).toUpperCase(), AirQualityEnum.FAIR);
            mapping.put(getAccessoryConfiguration("INFERIOR", settings.airQualityInferior).toUpperCase(),
                        AirQualityEnum.INFERIOR);
            mapping.put(getAccessoryConfiguration("POOR", settings.airQualityPoor).toUpperCase(), AirQualityEnum.POOR);
        }
        return mapping;
    }

    @Override
    public CompletableFuture<AirQualityEnum> getAirQuality() {
        AirQualityEnum quality = AirQualityEnum.UNKNOWN;
        final Optional<HomekitTaggedItem> characteristic = getCharacteristic(HomekitCharacteristicType.AIR_QUALITY);
        if (characteristic.isPresent()) {
            final String stringValue = characteristic.get().getItem().getState().toString();
            final AirQualityEnum value = getMapping().get(stringValue.toUpperCase());
            if (value != null) {
                quality = value;
            } else {
                logger.warn("Could not map air quality {} to supported air quality names {}. Item {}", stringValue,
                        mapping, getRootAccessory().getItem().getName());
            }
        } else {
            logger.warn("Missing mandatory characteristic {} at item {}", HomekitCharacteristicType.AIR_QUALITY,
                    getRootAccessory().getItem().getName());
        }
        return CompletableFuture.completedFuture(quality);
    }

    @Override
    public void subscribeAirQuality(final HomekitCharacteristicChangeCallback callback) {
        subscribe(HomekitCharacteristicType.AIR_QUALITY, callback);
    }

    @Override
    public void unsubscribeAirQuality() {
        unsubscribe(HomekitCharacteristicType.AIR_QUALITY);
    }
}
