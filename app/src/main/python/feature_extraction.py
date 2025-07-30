import numpy as np
from scipy.stats import skew, kurtosis

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

        features = (
                compute_axis_stats(ax) +
                compute_axis_stats(ay) +
                compute_axis_stats(az) +
                compute_axis_stats(gx) +
                compute_axis_stats(gy) +
                compute_axis_stats(gz)
        )
        print("Extracted features:", features)
        return features

    except Exception as e:
        print("Error in extract_features:", e)
        return [0.0] * 60

def compute_axis_stats(axis_data):
    try:
        axis = np.array(axis_data, dtype=np.float32)
        stats = [
            float(np.mean(axis)),
            float(np.std(axis)),
            float(np.min(axis)),
            float(np.max(axis)),
            float(np.median(axis)),
            float(skew(axis)),
            float(kurtosis(axis)),
            float(np.ptp(axis))
        ]
        print(f"Stats for axis {axis_data[:3]}...:", stats)
        return stats
    except Exception as e:
        print("Error in compute_axis_stats:", e)
        return [0.0] * 10

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

