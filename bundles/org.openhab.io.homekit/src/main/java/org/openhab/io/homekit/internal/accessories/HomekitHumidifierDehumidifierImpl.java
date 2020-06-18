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

import static org.openhab.io.homekit.internal.HomekitCharacteristicType.CURRENT_HUMIDIFIER_DEHUMIDIFIER_STATE;
import static org.openhab.io.homekit.internal.HomekitCharacteristicType.CURRENT_RELATIVE_HUMIDITY;
import static org.openhab.io.homekit.internal.HomekitCharacteristicType.TARGET_HUMIDIFIER_DEHUMIDIFIER_STATE;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.library.items.StringItem;
import org.eclipse.smarthome.core.library.items.SwitchItem;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.openhab.io.homekit.internal.HomekitAccessoryUpdater;
import org.openhab.io.homekit.internal.HomekitCharacteristicType;
import org.openhab.io.homekit.internal.HomekitSettings;
import org.openhab.io.homekit.internal.HomekitTaggedItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.hapjava.accessories.HumidifierDehumidifierAccessory;
import io.github.hapjava.characteristics.HomekitCharacteristicChangeCallback;
import io.github.hapjava.characteristics.impl.humidifier.CurrentHumidifierDehumidifierStateEnum;
import io.github.hapjava.characteristics.impl.humidifier.TargetHumidifierDehumidifierStateEnum;
import io.github.hapjava.services.impl.HumidifierDehumidifierService;

/**
 * Implements Heater Cooler
 *
 * @author Eugen Freiter - Initial contribution
 */

public class HomekitHumidifierDehumidifierImpl extends AbstractHomekitAccessoryImpl
        implements HumidifierDehumidifierAccessory {
    private final Logger logger = LoggerFactory.getLogger(HomekitHumidifierDehumidifierImpl.class);
    private final BooleanItemReader activeReader;
    private final Map<CurrentHumidifierDehumidifierStateEnum, String> currentStateMapping = new EnumMap(
            CurrentHumidifierDehumidifierStateEnum.class) {
        {
            put(CurrentHumidifierDehumidifierStateEnum.INACTIVE, "INACTIVE");
            put(CurrentHumidifierDehumidifierStateEnum.IDLE, "IDLE");
            put(CurrentHumidifierDehumidifierStateEnum.DEHUMIDIFYING, "DEHUMIDIFYING");
            put(CurrentHumidifierDehumidifierStateEnum.HUMIDIFYING, "HUMIDIFYING");

        }
    };
    private final Map<TargetHumidifierDehumidifierStateEnum, String> targetStateMapping = new EnumMap(
            TargetHumidifierDehumidifierStateEnum.class) {
        {
            put(TargetHumidifierDehumidifierStateEnum.AUTO, "AUTO");
            put(TargetHumidifierDehumidifierStateEnum.DEHUMIDIFIER, "DEHUMIDIFIER");
            put(TargetHumidifierDehumidifierStateEnum.HUMIDIFIER, "HUMIDIFIER");
        }
    };

    public HomekitHumidifierDehumidifierImpl(HomekitTaggedItem taggedItem,
            List<HomekitTaggedItem> mandatoryCharacteristics, HomekitAccessoryUpdater updater, HomekitSettings settings)
            throws IncompleteAccessoryException {
        super(taggedItem, mandatoryCharacteristics, updater, settings);
        activeReader = new BooleanItemReader(getItem(HomekitCharacteristicType.ACTIVE_STATUS, GenericItem.class),
                OnOffType.ON, OpenClosedType.OPEN);
        updateMapping(CURRENT_HUMIDIFIER_DEHUMIDIFIER_STATE, currentStateMapping);
        updateMapping(TARGET_HUMIDIFIER_DEHUMIDIFIER_STATE, targetStateMapping);
        getServices().add(new HumidifierDehumidifierService(this));
    }

    @Override
    public CompletableFuture<Double> getCurrentHumidity() {
        @Nullable
        DecimalType state = getStateAs(HomekitCharacteristicType.CURRENT_RELATIVE_HUMIDITY, DecimalType.class);
        if (state != null) {
            BigDecimal multiplicator = getAccessoryConfiguration(HomekitTaggedItem.MULTIPLIER, BigDecimal.valueOf(1.0));
            return CompletableFuture.completedFuture((state.toBigDecimal().multiply(multiplicator)).doubleValue());
        }
        return CompletableFuture.completedFuture(0.0);
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
    public CompletableFuture<CurrentHumidifierDehumidifierStateEnum> getCurrentHumidifierDehumidifierState() {
        return CompletableFuture.completedFuture(getKeyFromMapping(CURRENT_HUMIDIFIER_DEHUMIDIFIER_STATE,
                currentStateMapping, CurrentHumidifierDehumidifierStateEnum.INACTIVE));
    }

    @Override
    public CompletableFuture<TargetHumidifierDehumidifierStateEnum> getTargetHumidifierDehumidifierState() {
        return CompletableFuture.completedFuture(getKeyFromMapping(TARGET_HUMIDIFIER_DEHUMIDIFIER_STATE,
                targetStateMapping, TargetHumidifierDehumidifierStateEnum.AUTO));
    }

    @Override
    public CompletableFuture<Void> setTargetHumidifierDehumidifierState(
            final TargetHumidifierDehumidifierStateEnum state) {
        final Optional<HomekitTaggedItem> characteristic = getCharacteristic(
                HomekitCharacteristicType.TARGET_HUMIDIFIER_DEHUMIDIFIER_STATE);
        if (characteristic.isPresent()) {
            ((StringItem) characteristic.get().getItem()).send(new StringType(targetStateMapping.get(state)));
        } else {
            logger.warn("Missing mandatory characteristic {}",
                    HomekitCharacteristicType.TARGET_HUMIDIFIER_DEHUMIDIFIER_STATE.getTag());
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void subscribeCurrentHumidifierDehumidifierState(final HomekitCharacteristicChangeCallback callback) {
        subscribe(CURRENT_HUMIDIFIER_DEHUMIDIFIER_STATE, callback);
    }

    @Override
    public void unsubscribeCurrentHumidifierDehumidifierState() {
        unsubscribe(CURRENT_HUMIDIFIER_DEHUMIDIFIER_STATE);
    }

    @Override
    public void subscribeTargetHumidifierDehumidifierState(final HomekitCharacteristicChangeCallback callback) {
        subscribe(TARGET_HUMIDIFIER_DEHUMIDIFIER_STATE, callback);
    }

    @Override
    public void unsubscribeTargetHumidifierDehumidifierState() {
        unsubscribe(TARGET_HUMIDIFIER_DEHUMIDIFIER_STATE);
    }

    @Override
    public void subscribeActive(final HomekitCharacteristicChangeCallback callback) {
        subscribe(HomekitCharacteristicType.ACTIVE_STATUS, callback);
    }

    @Override
    public void unsubscribeActive() {
        unsubscribe(HomekitCharacteristicType.ACTIVE_STATUS);
    }

    @Override
    public void subscribeCurrentHumidity(final HomekitCharacteristicChangeCallback callback) {
        subscribe(CURRENT_RELATIVE_HUMIDITY, callback);
    }

    @Override
    public void unsubscribeCurrentHumidity() {
        unsubscribe(CURRENT_RELATIVE_HUMIDITY);
    }
}
