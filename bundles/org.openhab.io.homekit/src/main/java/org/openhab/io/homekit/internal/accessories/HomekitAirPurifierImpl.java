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

import static org.openhab.io.homekit.internal.HomekitCharacteristicType.ACTIVE_STATUS;
import static org.openhab.io.homekit.internal.HomekitCharacteristicType.CURRENT_AIR_PURIFIER_STATE;
import static org.openhab.io.homekit.internal.HomekitCharacteristicType.TARGET_AIR_PURIFIER_STATE;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.library.items.StringItem;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.openhab.io.homekit.internal.HomekitAccessoryUpdater;
import org.openhab.io.homekit.internal.HomekitSettings;
import org.openhab.io.homekit.internal.HomekitTaggedItem;

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
    private final BooleanItemReader activeReader;
    private final Map<CurrentAirPurifierStateEnum, String> currentStateMapping;
    private final Map<TargetAirPurifierStateEnum, String> targetStateMapping;

    public HomekitAirPurifierImpl(HomekitTaggedItem taggedItem, List<HomekitTaggedItem> mandatoryCharacteristics,
            HomekitAccessoryUpdater updater, HomekitSettings settings) throws IncompleteAccessoryException {
        super(taggedItem, mandatoryCharacteristics, updater, settings);
        currentStateMapping = new EnumMap<>(CurrentAirPurifierStateEnum.class);
        currentStateMapping.put(CurrentAirPurifierStateEnum.INACTIVE, "INACTIVE");
        currentStateMapping.put(CurrentAirPurifierStateEnum.PURIFYING_AIR, "PURIFYING");
        currentStateMapping.put(CurrentAirPurifierStateEnum.IDLE, "IDLE");
        updateMapping(CURRENT_AIR_PURIFIER_STATE, currentStateMapping);

        targetStateMapping = new EnumMap<>(TargetAirPurifierStateEnum.class);
        targetStateMapping.put(TargetAirPurifierStateEnum.MANUAL, "MANUAL");
        targetStateMapping.put(TargetAirPurifierStateEnum.AUTO, "AUTO");
        updateMapping(TARGET_AIR_PURIFIER_STATE, targetStateMapping);

        activeReader = new BooleanItemReader(getItem(ACTIVE_STATUS, GenericItem.class)
                .orElseThrow(() -> new IncompleteAccessoryException(ACTIVE_STATUS)), OnOffType.ON, OpenClosedType.OPEN);
        getServices().add(new AirPurifierService(this));
    }

    @Override
    public CompletableFuture<Boolean> isActive() {
        return CompletableFuture.completedFuture(activeReader.getValue());
    }

    @Override
    public CompletableFuture<Void> setActive(boolean state) {
        activeReader.setValue(state);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void subscribeActive(HomekitCharacteristicChangeCallback callback) {
        subscribe(ACTIVE_STATUS, callback);
    }

    @Override
    public void unsubscribeActive() {
        unsubscribe(ACTIVE_STATUS);
    }

    @Override
    public CompletableFuture<CurrentAirPurifierStateEnum> getCurrentState() {
        return CompletableFuture.completedFuture(getKeyFromMapping(CURRENT_AIR_PURIFIER_STATE, currentStateMapping,
                CurrentAirPurifierStateEnum.INACTIVE));
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
    public CompletableFuture<TargetAirPurifierStateEnum> getTargetState() {
        return CompletableFuture.completedFuture(
                getKeyFromMapping(TARGET_AIR_PURIFIER_STATE, targetStateMapping, TargetAirPurifierStateEnum.AUTO));
    }

    @Override
    public CompletableFuture<Void> setTargetState(final TargetAirPurifierStateEnum state) {
        getItem(TARGET_AIR_PURIFIER_STATE, StringItem.class)
                .ifPresent(item -> item.send(new StringType(targetStateMapping.get(state))));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void subscribeTargetState(final HomekitCharacteristicChangeCallback callback) {
        subscribe(TARGET_AIR_PURIFIER_STATE, callback);
    }

    @Override
    public void unsubscribeTargetState() {
        unsubscribe(TARGET_AIR_PURIFIER_STATE);
    }
}
