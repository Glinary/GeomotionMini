import numpy as np
from scipy.stats import skew, kurtosis
from scipy.signal import find_peaks


def extract_features(accel_data, gyro_data):
    try:
        print("Raw accel:", accel_data)
        print("Raw gyro:", gyro_data)

        accel = convert_java_data(accel_data)
        gyro = convert_java_data(gyro_data)

        print("Parsed accel:", accel)
        print("Parsed gyro:", gyro)

        if len(accel) < 3 or len(gyro) < 3:
            print("Not enough data.")
            return [0.0] * 60

        ax, ay, az = zip(*accel)
        gx, gy, gz = zip(*gyro)

        ax = np.array(ax, dtype=np.float32)
        ay = np.array(ay, dtype=np.float32)
        az = np.array(az, dtype=np.float32)
        gx = np.array(gx, dtype=np.float32)
        gy = np.array(gy, dtype=np.float32)
        gz = np.array(gz, dtype=np.float32)

        ayPeaks, _ = find_peaks(ay)
        azPeaks, _ = find_peaks(az)
        gxPeaks, _ = find_peaks(gx)
        gyPeaks, _ = find_peaks(gy)

        features = [
            # gyro_x_skewness
            float(skew(gx)),

            # y_curvature
            float(np.mean(np.abs(np.gradient(np.gradient(ay))))),

            # y_peak_count
            float(len(ayPeaks)),

            # x_var
            float(np.var(ax)),

            # x_rms
            float(np.sqrt(np.mean(ax**2))),

            # x_peak_to_peak
            float(np.max(ax) - np.min(ax)),

            # y_zcr
            float(np.sum(np.diff(np.sign(ay)) != 0)),

            # gyro_x_peak_count
            float(len(gxPeaks)),

            # gyro_y_skewness
            float(skew(gy)),

            # z_peak_count
            float(len(azPeaks)),

            # z_mean
            float(np.mean(az)),

            # x_zcr
            float(np.sum(np.diff(np.sign(ax)) != 0)),

            # gyro_y_var
            float(np.var(gy)),

            # gyro_z_zcr
            float(np.sum(np.diff(np.sign(gz)) != 0)),

            # gyro_z_var
            float(np.var(gz)),

            # z_skewness
            float(skew(az)),

            # gyro_y_rms
            float(np.sqrt(np.mean(gy**2))),

            # gyro_z_peak_to_peak
            float(np.max(gz) - np.min(gz)),

            # y_var
            float(np.var(ay)),

            # gyro_x_rms
            float(np.sqrt(np.mean(gx**2))),

            # y_rms
            float(np.sqrt(np.mean(ay**2))),

            # gyro_y_mean
            float(np.mean(gy)),

            # gyro_z_kurtosis
            float(kurtosis(gz)),

            # y_skewness
            float(skew(ay)),

            # gyro_y_zcr
            float(np.sum(np.diff(np.sign(gy)) != 0)),

            # gyro_y_peak_to_peak
            float(np.max(gy) - np.min(gy)),

            # y_peak_to_peak
            float(np.max(ay) - np.min(ay)),

            # gyro_x_peak_to_peak
            float(np.max(gx) - np.min(gx)),

            # z_kurtosis
            float(kurtosis(az)),

            # gyro_x_curvature
            float(np.mean(np.abs(np.gradient(np.gradient(gx)))))
        ]

        print("Final features:", features)
        return features

    except Exception as e:
        print("Error in extract_features:", e)
        return [0.0] * 30  #best num of features


def convert_java_data(java_list):
    try:
        result = []
        size = java_list.size()
        for i in range(size):
            point = java_list.get(i)  # Java ArrayList<Float>
            inner = []
            for j in range(point.size()):
                inner.append(float(point.get(j)))
            result.append(inner)
        return result
    except Exception as e:
        print("Error in convert_java_data:", e)
        return []


