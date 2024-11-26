package com.mobdeve.s11.mco3.mco3javaversion;

public class ButterworthFilter {
    private final double[] a; // Feedback coefficients
    private final double[] b; // Feedforward coefficients
    private final double[] xHistory; // Input history
    private final double[] yHistory; // Output history

    public ButterworthFilter(double[] bCoefficients, double[] aCoefficients) {
        this.b = bCoefficients;
        this.a = aCoefficients;
        this.xHistory = new double[b.length];
        this.yHistory = new double[a.length - 1];
    }

    public double apply(double x) {
        // Shift the history buffers
        System.arraycopy(xHistory, 0, xHistory, 1, xHistory.length - 1);
        System.arraycopy(yHistory, 0, yHistory, 1, yHistory.length - 1);

        xHistory[0] = x;

        // Apply the filter equation
        double y = 0.0;
        for (int i = 0; i < b.length; i++) {
            y += b[i] * xHistory[i];
        }
        for (int i = 1; i < a.length; i++) {
            y -= a[i] * yHistory[i - 1];
        }

        y /= a[0];
        yHistory[0] = y;

        return y;
    }
}
