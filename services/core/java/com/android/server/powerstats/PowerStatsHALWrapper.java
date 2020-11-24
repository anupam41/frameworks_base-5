/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.powerstats;

import android.hardware.power.stats.IPowerStats;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog;

import com.android.internal.annotations.GuardedBy;

import java.util.function.Supplier;

/**
 * PowerStatsHALWrapper is a wrapper class for the PowerStats HAL API calls.
 */
public final class PowerStatsHALWrapper {
    private static final String TAG = PowerStatsHALWrapper.class.getSimpleName();

    /**
     * IPowerStatsHALWrapper defines the interface to the PowerStatsHAL.
     */
    public interface IPowerStatsHALWrapper {
        /**
         * Returns information related to all supported PowerEntity(s) for which state residency
         * data is available.
         *
         * A PowerEntity is defined as a platform subsystem, peripheral, or power domain that
         * impacts the total device power consumption.
         *
         * @return List of information on each PowerEntity.
         */
        android.hardware.power.stats.PowerEntityInfo[] getPowerEntityInfo();

        /**
         * Reports the accumulated state residency for each requested PowerEntity.
         *
         * Each PowerEntity may reside in one of multiple states. It may also transition from one
         * state to another. StateResidency is defined as an accumulation of time that a
         * PowerEntity resided in each of its possible states, the number of times that each state
         * was entered, and a timestamp corresponding to the last time that state was entered.
         *
         * Data is accumulated starting at device boot.
         *
         * @param powerEntityIds List of IDs of PowerEntities for which data is requested.  Passing
         *                       an empty list will return state residency for all available
         *                       PowerEntities.  ID of each PowerEntity is contained in
         *                       PowerEntityInfo.
         *
         * @return StateResidency since boot for each requested PowerEntity
         */
        android.hardware.power.stats.StateResidencyResult[] getStateResidency(int[] powerEntityIds);

        /**
         * Returns the energy consumer IDs for all available energy consumers (power models) on the
         * device.  Examples of subsystems for which energy consumer results (power models) may be
         * available are GPS, display, wifi, etc.  The default list of energy consumers can be
         * found in the PowerStats HAL definition (EnergyConsumerId.aidl).  The availability of
         * energy consumer IDs is hardware dependent.
         *
         * @return List of EnergyConsumerIds all available energy consumers.
         */
        int[] getEnergyConsumerInfo();

        /**
         * Returns the energy consumer result for all available energy consumers (power models).
         * Available consumers can be retrieved by calling getEnergyConsumerInfo().  The subsystem
         * corresponding to the energy consumer result is defined by the energy consumer ID.
         *
         * @param energyConsumerIds Array of energy consumer IDs for which energy consumed is being
         *                          requested.  Energy consumers available on the device can be
         *                          queried by calling getEnergyConsumerInfo().  Passing an empty
         *                          array will return results for all energy consumers.
         *
         * @return List of EnergyConsumerResult objects containing energy consumer results for all
         *         available energy consumers (power models).
         */
        android.hardware.power.stats.EnergyConsumerResult[] getEnergyConsumed(
                int[] energyConsumerIds);

        /**
         * Returns channel info for all available energy meters.
         *
         * @return List of ChannelInfo objects containing channel info for all available energy
         *         meters.
         */
        android.hardware.power.stats.ChannelInfo[] getEnergyMeterInfo();

        /**
         * Returns energy measurements for all available energy meters.  Available channels can be
         * retrieved by calling getEnergyMeterInfo().  Energy measurements and channel info can be
         * linked through the channelId field.
         *
         * @param channelIds Array of channel IDs for which energy measurements are being requested.
         *                   Channel IDs available on the device can be queried by calling
         *                   getEnergyMeterInfo().  Passing an empty array will return energy
         *                   measurements for all channels.
         *
         * @return List of EnergyMeasurement objects containing energy measurements for all
         *         available energy meters.
         */
        android.hardware.power.stats.EnergyMeasurement[] readEnergyMeters(int[] channelIds);

        /**
         * Returns boolean indicating if connection to power stats HAL was established.
         *
         * @return true if connection to power stats HAL was correctly established.
         */
        boolean initialize();
    }

    /**
     * PowerStatsHALWrapperImpl is the implementation of the IPowerStatsHALWrapper
     * used by the PowerStatsService.  Other implementations will be used by the testing
     * framework and will be passed into the PowerStatsService through an injector.
     */
    public static final class PowerStatsHALWrapperImpl implements IPowerStatsHALWrapper {
        private static Supplier<IPowerStats> sVintfPowerStats;

        @Override
        public android.hardware.power.stats.PowerEntityInfo[] getPowerEntityInfo() {
            android.hardware.power.stats.PowerEntityInfo[] powerEntityInfoHAL = null;

            if (sVintfPowerStats != null) {
                try {
                    powerEntityInfoHAL = sVintfPowerStats.get().getPowerEntityInfo();
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failed to get power entity info from PowerStats HAL");
                }
            }

            return powerEntityInfoHAL;
        }

        @Override
        public android.hardware.power.stats.StateResidencyResult[] getStateResidency(
                int[] powerEntityIds) {
            android.hardware.power.stats.StateResidencyResult[] stateResidencyResultHAL = null;

            if (sVintfPowerStats != null) {
                try {
                    stateResidencyResultHAL =
                        sVintfPowerStats.get().getStateResidency(powerEntityIds);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failed to get state residency from PowerStats HAL");
                }
            }

            return stateResidencyResultHAL;
        }

        @Override
        public int[] getEnergyConsumerInfo() {
            int[] energyConsumerInfoHAL = null;

            if (sVintfPowerStats != null) {
                try {
                    energyConsumerInfoHAL = sVintfPowerStats.get().getEnergyConsumerInfo();
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failed to get energy consumer info from PowerStats HAL");
                }
            }

            return energyConsumerInfoHAL;
        }

        @Override
        public android.hardware.power.stats.EnergyConsumerResult[] getEnergyConsumed(
                int[] energyConsumerIds) {
            android.hardware.power.stats.EnergyConsumerResult[] energyConsumedHAL = null;

            if (sVintfPowerStats != null) {
                try {
                    energyConsumedHAL =
                        sVintfPowerStats.get().getEnergyConsumed(energyConsumerIds);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failed to get energy consumer results from PowerStats HAL");
                }
            }

            return energyConsumedHAL;
        }

        @Override
        public android.hardware.power.stats.ChannelInfo[] getEnergyMeterInfo() {
            android.hardware.power.stats.ChannelInfo[] energyMeterInfoHAL = null;

            if (sVintfPowerStats != null) {
                try {
                    energyMeterInfoHAL = sVintfPowerStats.get().getEnergyMeterInfo();
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failed to get energy meter info from PowerStats HAL");
                }
            }

            return energyMeterInfoHAL;
        }

        @Override
        public android.hardware.power.stats.EnergyMeasurement[] readEnergyMeters(int[] channelIds) {
            android.hardware.power.stats.EnergyMeasurement[] energyMeasurementHAL = null;

            if (sVintfPowerStats != null) {
                try {
                    energyMeasurementHAL =
                        sVintfPowerStats.get().readEnergyMeters(channelIds);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failed to get energy measurements from PowerStats HAL");
                }
            }

            return energyMeasurementHAL;
        }

        @Override
        public boolean initialize() {
            Supplier<IPowerStats> service = new VintfHalCache();

            if (service.get() == null) {
                sVintfPowerStats = null;
                return false;
            } else {
                sVintfPowerStats = service;
                return true;
            }
        }
    }

    private static class VintfHalCache implements Supplier<IPowerStats>, IBinder.DeathRecipient {
        @GuardedBy("this")
        private IPowerStats mInstance = null;

        @Override
        public synchronized IPowerStats get() {
            if (mInstance == null) {
                IBinder binder = Binder.allowBlocking(ServiceManager.waitForDeclaredService(
                        "android.hardware.power.stats.IPowerStats/default"));
                if (binder != null) {
                    mInstance = IPowerStats.Stub.asInterface(binder);
                    try {
                        binder.linkToDeath(this, 0);
                    } catch (RemoteException e) {
                        Slog.e(TAG, "Unable to register DeathRecipient for " + mInstance);
                    }
                }
            }
            return mInstance;
        }

        @Override
        public synchronized void binderDied() {
            mInstance = null;
        }
    }
}
