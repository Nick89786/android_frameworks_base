/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.media;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * The AudioRecordConfiguration class collects the information describing an audio recording
 * session. This information is returned through the
 * {@link AudioManager#getActiveRecordConfigurations()} method.
 *
 */
public class AudioRecordConfiguration implements Parcelable {

    private final int mSessionId;

    private final int mClientSource;

    private final AudioFormat mDeviceFormat;
    private final AudioFormat mClientFormat;

    private final AudioDeviceInfo mRecDevice;

    /**
     * @hide
     */
    public AudioRecordConfiguration(int session, int source) {
        mSessionId = session;
        mClientSource = source;
        mDeviceFormat = new AudioFormat.Builder().build();
        mClientFormat = new AudioFormat.Builder().build();
        mRecDevice = null;
    }

    /**
     * @hide
     */
    public AudioRecordConfiguration(int session, int source, AudioFormat devFormat,
            AudioFormat clientFormat, AudioDeviceInfo device) {
        mSessionId = session;
        mClientSource = source;
        mDeviceFormat = devFormat;
        mClientFormat = clientFormat;
        mRecDevice = device;
    }

    /**
     * Returns the audio source being used for the recording.
     * @return one of {@link MediaRecorder.AudioSource#MIC},
     *       {@link MediaRecorder.AudioSource#VOICE_UPLINK},
     *       {@link MediaRecorder.AudioSource#VOICE_DOWNLINK},
     *       {@link MediaRecorder.AudioSource#VOICE_CALL},
     *       {@link MediaRecorder.AudioSource#CAMCORDER},
     *       {@link MediaRecorder.AudioSource#VOICE_RECOGNITION},
     *       {@link MediaRecorder.AudioSource#VOICE_COMMUNICATION}.
     */
    public int getClientAudioSource() { return mClientSource; }

    /**
     * Returns the session number of the recording, see {@link AudioRecord#getAudioSessionId()}.
     * @return the session number.
     */
    public int getClientAudioSessionId() { return mSessionId; }

    /**
     * Returns the audio format at which audio is recorded on this Android device.
     * Note that it may differ from the client application recording format
     * (see {@link #getClientFormat()}).
     * @return the device recording format
     */
    public AudioFormat getFormat() { return mDeviceFormat; }

    /**
     * Returns the audio format at which the client application is recording audio.
     * Note that it may differ from the actual recording format (see {@link #getFormat()}).
     * @return the recording format
     */
    public AudioFormat getClientFormat() { return mClientFormat; }

    /**
     * Returns the audio input device used for this recording.
     * @return the audio recording device
     */
    public AudioDeviceInfo getAudioDevice() { return mRecDevice; }

    public static final Parcelable.Creator<AudioRecordConfiguration> CREATOR
            = new Parcelable.Creator<AudioRecordConfiguration>() {
        /**
         * Rebuilds an AudioRecordConfiguration previously stored with writeToParcel().
         * @param p Parcel object to read the AudioRecordConfiguration from
         * @return a new AudioRecordConfiguration created from the data in the parcel
         */
        public AudioRecordConfiguration createFromParcel(Parcel p) {
            return new AudioRecordConfiguration(p);
        }
        public AudioRecordConfiguration[] newArray(int size) {
            return new AudioRecordConfiguration[size];
        }
    };

    @Override
    public int hashCode() {
        return Objects.hash(mSessionId, mClientSource);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mSessionId);
        dest.writeInt(mClientSource);
    }

    private AudioRecordConfiguration(Parcel in) {
        mSessionId = in.readInt();
        mClientSource = in.readInt();
        mDeviceFormat = new AudioFormat.Builder().build();
        mClientFormat = new AudioFormat.Builder().build();
        mRecDevice = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof AudioRecordConfiguration)) return false;

        final AudioRecordConfiguration that = (AudioRecordConfiguration) o;
         return ((mSessionId == that.mSessionId)
                 && (mClientSource == that.mClientSource));
    }
}