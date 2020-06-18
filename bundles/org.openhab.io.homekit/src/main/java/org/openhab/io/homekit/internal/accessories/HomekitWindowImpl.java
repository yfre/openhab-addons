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

import static org.openhab.io.homekit.internal.HomekitCharacteristicType.CURRENT_POSITION;
import static org.openhab.io.homekit.internal.HomekitCharacteristicType.POSITION_STATE;
import static org.openhab.io.homekit.internal.HomekitCharacteristicType.TARGET_POSITION;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.library.items.DimmerItem;
import org.eclipse.smarthome.core.library.items.NumberItem;
import org.eclipse.smarthome.core.library.items.RollershutterItem;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.openhab.io.homekit.internal.HomekitAccessoryUpdater;
import org.openhab.io.homekit.internal.HomekitCharacteristicType;
import org.openhab.io.homekit.internal.HomekitSettings;
import org.openhab.io.homekit.internal.HomekitTaggedItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.hapjava.accessories.WindowAccessory;
import io.github.hapjava.characteristics.HomekitCharacteristicChangeCallback;
import io.github.hapjava.characteristics.impl.windowcovering.PositionStateEnum;
import io.github.hapjava.services.impl.WindowService;

/**
 *
 * @author Eugen Freiter - Initial contribution
 */
public class HomekitWindowImpl extends AbstractHomekitAccessoryImpl implements WindowAccessory {
    private final Logger logger = LoggerFactory.getLogger(HomekitWindowImpl.class);
    // inverts the value of position.
    private static final String CONFIG_INVERT = "inverted";
    private final int inverted;
    private final Map<PositionStateEnum, String> positionStateMapping = new EnumMap(PositionStateEnum.class) {
        {
            put(PositionStateEnum.DECREASING, "DECREASING");
            put(PositionStateEnum.INCREASING, "INCREASING");
            put(PositionStateEnum.STOPPED, "STOPPED");
        }
    };

    public HomekitWindowImpl(HomekitTaggedItem taggedItem, List<HomekitTaggedItem> mandatoryCharacteristics,
            HomekitAccessoryUpdater updater, HomekitSettings settings) throws IncompleteAccessoryException {
        super(taggedItem, mandatoryCharacteristics, updater, settings);
        updateMapping(POSITION_STATE, positionStateMapping);
        final String invertedConfig = getAccessoryConfiguration(CONFIG_INVERT, "false");
        inverted = invertedConfig.equalsIgnoreCase("yes") || invertedConfig.equalsIgnoreCase("true") ? 1 : 0;
        this.getServices().add(new WindowService(this));
    }

    @Override
    public CompletableFuture<Integer> getCurrentPosition() {
        return CompletableFuture.completedFuture(convertValue(getStateAs(CURRENT_POSITION, DecimalType.class)));
    }

    @Override
    public CompletableFuture<PositionStateEnum> getPositionState() {
        return CompletableFuture
                .completedFuture(getKeyFromMapping(POSITION_STATE, positionStateMapping, PositionStateEnum.STOPPED));
    }

    @Override
    public CompletableFuture<Integer> getTargetPosition() {
        return CompletableFuture.completedFuture(convertValue(getStateAs(TARGET_POSITION, DecimalType.class)));
    }

    @Override
    public CompletableFuture<Void> setTargetPosition(final Integer value) throws Exception {
        final Optional<HomekitTaggedItem> taggedItem = getCharacteristic(TARGET_POSITION);
        if (taggedItem.isPresent()) {
            final Item item = taggedItem.get().getItem();
            final int targetPosition = Math.abs(value.intValue() - 100 * inverted);
            if (item instanceof DimmerItem) {
                ((DimmerItem) item).send(new PercentType(targetPosition));
            } else if (item instanceof NumberItem) {
                ((NumberItem) item).send(new DecimalType(targetPosition));
            } else if (item instanceof RollershutterItem) {
                ((RollershutterItem) item).send(new PercentType(targetPosition));
            } else {
                logger.debug("unsupported type of item {}. Expected Dimmer, Number or Rollershutter", item);
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void subscribeCurrentPosition(HomekitCharacteristicChangeCallback callback) {
        subscribe(CURRENT_POSITION, callback);
    }

    @Override
    public void subscribePositionState(HomekitCharacteristicChangeCallback callback) {
        subscribe(POSITION_STATE, callback);
    }

    @Override
    public void subscribeTargetPosition(HomekitCharacteristicChangeCallback callback) {
        subscribe(HomekitCharacteristicType.TARGET_POSITION, callback);
    }

    @Override
    public void unsubscribeCurrentPosition() {
        unsubscribe(CURRENT_POSITION);
    }

    @Override
    public void unsubscribePositionState() {
        unsubscribe(POSITION_STATE);
    }

    @Override
    public void unsubscribeTargetPosition() {
        unsubscribe(CURRENT_POSITION);
    }

    private int convertValue(Number value) {
        return value != null ? Math.abs(value.intValue() - 100 * inverted) : 100 * inverted;
    }
}
