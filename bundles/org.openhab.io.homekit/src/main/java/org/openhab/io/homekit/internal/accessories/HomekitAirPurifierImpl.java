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

import static org.openhab.io.homekit.internal.HomekitCharacteristicType.CURRENT_AIR_PURIFIER_STATE;
import static org.openhab.io.homekit.internal.HomekitCharacteristicType.TARGET_AIR_PURIFIER_STATE;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.library.items.StringItem;
import org.eclipse.smarthome.core.library.items.SwitchItem;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.openhab.io.homekit.internal.HomekitAccessoryUpdater;
import org.openhab.io.homekit.internal.HomekitCharacteristicType;
import org.openhab.io.homekit.internal.HomekitSettings;
import org.openhab.io.homekit.internal.HomekitTaggedItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.hapjava.accessories.AirPurifierAccessory;
import io.github.hapjava.characteristics.HomekitCharacteristicChangeCallback;
import io.github.hapjava.characteristics.impl.airpurifier.CurrentAirPurifierStateEnum;
import io.github.hapjava.characteristics.impl.airpurifier.TargetAirPurifierStateEnum;
import io.github.hapjava.services.impl.AirPurifierService;

/**
 * Implements Air Purifier
 *
 * @author Eugen Freiter - Initial contribution
 */

public class HomekitAirPurifierImpl extends AbstractHomekitAccessoryImpl implements AirPurifierAccessory {
    private final Logger logger = LoggerFactory.getLogger(HomekitAirPurifierImpl.class);
    private final BooleanItemReader activeReader;
    private final Map<CurrentAirPurifierStateEnum, String> currentStateMapping = new EnumMap(
            CurrentAirPurifierStateEnum.class) {
        {
            put(CurrentAirPurifierStateEnum.INACTIVE, "INACTIVE");
            put(CurrentAirPurifierStateEnum.IDLE, "IDLE");
            put(CurrentAirPurifierStateEnum.PURIFYING_AIR, "PURIFYING");
        }
    };
    private final Map<TargetAirPurifierStateEnum, String> targetStateMapping = new EnumMap(
            TargetAirPurifierStateEnum.class) {
        {
            put(TargetAirPurifierStateEnum.AUTO, "AUTO");
            put(TargetAirPurifierStateEnum.MANUAL, "MANUAL");
        }
    };

    public HomekitAirPurifierImpl(HomekitTaggedItem taggedItem, List<HomekitTaggedItem> mandatoryCharacteristics,
            HomekitAccessoryUpdater updater, HomekitSettings settings) throws IncompleteAccessoryException {
        super(taggedItem, mandatoryCharacteristics, updater, settings);
        activeReader = new BooleanItemReader(getItem(HomekitCharacteristicType.ACTIVE_STATUS, GenericItem.class),
                OnOffType.ON, OpenClosedType.OPEN);
        updateMapping(CURRENT_AIR_PURIFIER_STATE, currentStateMapping);
        updateMapping(TARGET_AIR_PURIFIER_STATE, targetStateMapping);
        getServices().add(new AirPurifierService(this));
    }

    @Override
    public CompletableFuture<Boolean> isActive() {
        return CompletableFuture.completedFuture(activeReader.getValue() == Boolean.TRUE);
    }

    @Override
    public CompletableFuture<Void> setActive(final boolean state) {
        final @Nullable SwitchItem item = getItem(HomekitCharacteristicType.ACTIVE_STATUS, SwitchItem.class);
        if (item != null) {
            item.send(OnOffType.from(state));
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<CurrentAirPurifierStateEnum> getCurrentState() {
        return CompletableFuture.completedFuture(getKeyFromMapping(CURRENT_AIR_PURIFIER_STATE, currentStateMapping,
                CurrentAirPurifierStateEnum.INACTIVE));
    }

    @Override
    public CompletableFuture<TargetAirPurifierStateEnum> getTargetState() {
        return CompletableFuture.completedFuture(
                getKeyFromMapping(TARGET_AIR_PURIFIER_STATE, targetStateMapping, TargetAirPurifierStateEnum.AUTO));
    }

    @Override
    public CompletableFuture<Void> setTargetState(final TargetAirPurifierStateEnum state) {
        final Optional<HomekitTaggedItem> characteristic = getCharacteristic(TARGET_AIR_PURIFIER_STATE);
        if (characteristic.isPresent()) {
            ((StringItem) characteristic.get().getItem()).send(new StringType(targetStateMapping.get(state)));
        } else {
            logger.warn("Missing mandatory characteristic {}", TARGET_AIR_PURIFIER_STATE.getTag());
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void subscribeCurrentState(final HomekitCharacteristicChangeCallback callback) {
        subscribe(CURRENT_AIR_PURIFIER_STATE, callback);
    }

    @Override
    public void unsubscribeCurrentState() {
        unsubscribe(CURRENT_AIR_PURIFIER_STATE);
    }

    @Override
    public void subscribeTargetState(final HomekitCharacteristicChangeCallback callback) {
        subscribe(TARGET_AIR_PURIFIER_STATE, callback);
    }

    @Override
    public void unsubscribeTargetState() {
        unsubscribe(TARGET_AIR_PURIFIER_STATE);
    }

    @Override
    public void subscribeActive(final HomekitCharacteristicChangeCallback callback) {
        subscribe(HomekitCharacteristicType.ACTIVE_STATUS, callback);
    }

    @Override
    public void unsubscribeActive() {
        unsubscribe(HomekitCharacteristicType.ACTIVE_STATUS);
    }
}
