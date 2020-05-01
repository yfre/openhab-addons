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

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.items.StringItem;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.io.homekit.internal.HomekitAccessoryUpdater;
import org.openhab.io.homekit.internal.HomekitCharacteristicType;
import org.openhab.io.homekit.internal.HomekitSettings;
import org.openhab.io.homekit.internal.HomekitTaggedItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.hapjava.accessories.SecuritySystemAccessory;
import io.github.hapjava.characteristics.HomekitCharacteristicChangeCallback;
import io.github.hapjava.characteristics.impl.securitysystem.CurrentSecuritySystemStateEnum;
import io.github.hapjava.characteristics.impl.securitysystem.TargetSecuritySystemStateEnum;
import io.github.hapjava.services.impl.SecuritySystemService;

/**
 * Implements SecuritySystem as a GroupedAccessory made up of multiple items:
 * <ul>
 * <li>CurrentSecuritySystemState: String type</li>
 * <li>TargetSecuritySystemState: String type</li>
 * </ul>
 * 
 * @author Cody Cutrer - Initial contribution
 */
public class HomekitSecuritySystemImpl extends AbstractHomekitAccessoryImpl implements SecuritySystemAccessory {
    private final Logger LOGGER = LoggerFactory.getLogger(HomekitSecuritySystemImpl.class);

    public HomekitSecuritySystemImpl(HomekitTaggedItem taggedItem, List<HomekitTaggedItem> mandatoryCharacteristics,
            HomekitAccessoryUpdater updater, HomekitSettings settings) throws IncompleteAccessoryException {
        super(taggedItem, mandatoryCharacteristics, updater, settings);
        getServices().add(new SecuritySystemService(this));
    }

    @Override
    public CompletableFuture<CurrentSecuritySystemStateEnum> getCurrentSecuritySystemState() {
        CurrentSecuritySystemStateEnum state;
        final @Nullable State itemState = getStateAs(HomekitCharacteristicType.SECURITY_SYSTEM_CURRENT_STATE,
                StringType.class);
        if (itemState != null) {
            String stringValue = itemState.toString();
            if (stringValue.equalsIgnoreCase("DISARMED")) {
                state = CurrentSecuritySystemStateEnum.DISARMED;
            } else if (stringValue.equalsIgnoreCase("AWAY_ARM")) {
                state = CurrentSecuritySystemStateEnum.AWAY_ARM;
            } else if (stringValue.equalsIgnoreCase("STAY_ARM")) {
                state = CurrentSecuritySystemStateEnum.STAY_ARM;
            } else if (stringValue.equalsIgnoreCase("NIGHT_ARM")) {
                state = CurrentSecuritySystemStateEnum.NIGHT_ARM;
            } else if (stringValue.equalsIgnoreCase("TRIGGERED")) {
                state = CurrentSecuritySystemStateEnum.TRIGGERED;
            } else if (stringValue.equals("UNDEF") || stringValue.equals("NULL")) {
                LOGGER.debug("Security system target state not available. Relaying value of DISARM to HomeKit");
                state = CurrentSecuritySystemStateEnum.DISARMED;
            } else {
                LOGGER.error(
                        "Unrecognized security system target state: {}. Expected DISARM, AWAY_ARM, STAY_ARM, NIGHT_ARM strings in value.",
                        stringValue);
                state = CurrentSecuritySystemStateEnum.DISARMED;
            }
        } else {
            LOGGER.debug("Security system target state not available. Relaying value of DISARM to HomeKit");
            state = CurrentSecuritySystemStateEnum.DISARMED;
        }
        return CompletableFuture.completedFuture(state);
    }

    @Override
    public void setTargetSecuritySystemState(final TargetSecuritySystemStateEnum state) {
        final @Nullable StringItem item = getItem(HomekitCharacteristicType.SECURITY_SYSTEM_TARGET_STATE,
                StringItem.class);
        if (item != null)
            item.send(new StringType(state.toString()));
        else
            LOGGER.warn("Item for target security state at accessory {} not found.", this.getName());
    }

    @Override
    public CompletableFuture<TargetSecuritySystemStateEnum> getTargetSecuritySystemState() {
        TargetSecuritySystemStateEnum state;

        final @Nullable State itemState = getStateAs(HomekitCharacteristicType.SECURITY_SYSTEM_TARGET_STATE,
                StringType.class);
        if (itemState != null) {
            String stringValue = itemState.toString();
            if (stringValue.equalsIgnoreCase("DISARM")) {
                state = TargetSecuritySystemStateEnum.DISARM;
            } else if (stringValue.equalsIgnoreCase("AWAY_ARM")) {
                state = TargetSecuritySystemStateEnum.AWAY_ARM;
            } else if (stringValue.equalsIgnoreCase("STAY_ARM")) {
                state = TargetSecuritySystemStateEnum.STAY_ARM;
            } else if (stringValue.equalsIgnoreCase("NIGHT_ARM")) {
                state = TargetSecuritySystemStateEnum.NIGHT_ARM;
            } else if (stringValue.equals("UNDEF") || stringValue.equals("NULL")) {
                LOGGER.debug("Security system target state not available. Relaying value of DISARM to HomeKit");
                state = TargetSecuritySystemStateEnum.DISARM;
            } else {
                LOGGER.error(
                        "Unrecognized security system target state: {}. Expected DISARM, AWAY_ARM, STAY_ARM, NIGHT_ARM strings in value.",
                        stringValue);
                state = TargetSecuritySystemStateEnum.DISARM;
            }
        } else {
            LOGGER.debug("Security system target state not available. Relaying value of DISARM to HomeKit");
            state = TargetSecuritySystemStateEnum.DISARM;
        }
        return CompletableFuture.completedFuture(state);
    }

    @Override
    public void subscribeCurrentSecuritySystemState(final HomekitCharacteristicChangeCallback callback) {
        subscribe(HomekitCharacteristicType.SECURITY_SYSTEM_CURRENT_STATE, callback);
    }

    @Override
    public void unsubscribeCurrentSecuritySystemState() {
        unsubscribe(HomekitCharacteristicType.SECURITY_SYSTEM_CURRENT_STATE);
    }

    @Override
    public void subscribeTargetSecuritySystemState(final HomekitCharacteristicChangeCallback callback) {
        subscribe(HomekitCharacteristicType.SECURITY_SYSTEM_TARGET_STATE, callback);
    }

    @Override
    public void unsubscribeTargetSecuritySystemState() {
        unsubscribe(HomekitCharacteristicType.SECURITY_SYSTEM_TARGET_STATE);
    }
}
