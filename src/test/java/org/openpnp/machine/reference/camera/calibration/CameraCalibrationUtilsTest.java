package org.openpnp.machine.reference.camera.calibration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Covers the residual error arithmetic of the camera calibration solver.
 * <p>
 * Points are indexed as {@code [calibrationHeight][point][x|y]}. The outlier list, however, is
 * indexed by a single counter that runs across every height, which is the detail most likely to be
 * got wrong by a caller.
 */
public class CameraCalibrationUtilsTest {
    private static final double DELTA = 1e-9;

    /** Two calibration heights of two points each, so outlier indices 0..3. */
    private static double[][][] actual() {
        return new double[][][] {
                {{10, 20}, {30, 40}},
                {{50, 60}, {70, 80}},
        };
    }

    /** Modeled points offset from the actual ones by a known amount per point. */
    private static double[][][] modeled() {
        return new double[][][] {
                {{9, 18}, {27, 36}},
                {{45, 54}, {63, 72}},
        };
    }

    private static ArrayList<Integer> outliers(Integer... indices) {
        return new ArrayList<>(Arrays.asList(indices));
    }

    @Test
    public void residualIsActualMinusModeled() {
        List<double[]> residuals =
                CameraCalibrationUtils.computeResidualErrors(actual(), modeled());

        assertEquals(4, residuals.size());
        assertArrayEquals2D(new double[] {1, 2}, residuals.get(0));
        assertArrayEquals2D(new double[] {3, 4}, residuals.get(1));
        assertArrayEquals2D(new double[] {5, 6}, residuals.get(2));
        assertArrayEquals2D(new double[] {7, 8}, residuals.get(3));
    }

    @Test
    public void heightIndexSelectsASingleCalibrationHeight() {
        List<double[]> residuals =
                CameraCalibrationUtils.computeResidualErrors(actual(), modeled(), Integer.valueOf(1));

        assertEquals(2, residuals.size());
        assertArrayEquals2D(new double[] {5, 6}, residuals.get(0));
        assertArrayEquals2D(new double[] {7, 8}, residuals.get(1));
    }

    @Test
    public void nullHeightIndexKeepsEveryHeight() {
        List<double[]> residuals = CameraCalibrationUtils.computeResidualErrors(actual(), modeled(),
                (Integer) null);

        assertEquals(4, residuals.size());
    }

    @Test
    public void outlierIndicesRunAcrossHeights() {
        // Index 2 is the first point of the second height, not the third point of the first.
        List<double[]> residuals =
                CameraCalibrationUtils.computeResidualErrors(actual(), modeled(), outliers(2));

        assertEquals(3, residuals.size());
        assertArrayEquals2D(new double[] {1, 2}, residuals.get(0));
        assertArrayEquals2D(new double[] {3, 4}, residuals.get(1));
        assertArrayEquals2D(new double[] {7, 8}, residuals.get(2));
    }

    @Test
    public void severalOutliersAreExcluded() {
        List<double[]> residuals =
                CameraCalibrationUtils.computeResidualErrors(actual(), modeled(), outliers(0, 3));

        assertEquals(2, residuals.size());
        assertArrayEquals2D(new double[] {3, 4}, residuals.get(0));
        assertArrayEquals2D(new double[] {5, 6}, residuals.get(1));
    }

    @Test
    public void outlierIndexOutsideTheSelectedHeightStillCounts() {
        // Selecting height 1 while excluding index 0, which lives in height 0: the exclusion has
        // no visible effect, but it must not shift the numbering of the remaining points.
        List<double[]> residuals = CameraCalibrationUtils.computeResidualErrors(actual(), modeled(),
                Integer.valueOf(1), outliers(0));

        assertEquals(2, residuals.size());
        assertArrayEquals2D(new double[] {5, 6}, residuals.get(0));
        assertArrayEquals2D(new double[] {7, 8}, residuals.get(1));
    }

    @Test
    public void heightAndOutlierFiltersCombine() {
        List<double[]> residuals = CameraCalibrationUtils.computeResidualErrors(actual(), modeled(),
                Integer.valueOf(1), outliers(2));

        assertEquals(1, residuals.size());
        assertArrayEquals2D(new double[] {7, 8}, residuals.get(0));
    }

    @Test
    public void nullOutlierListBehavesLikeAnEmptyOne() {
        List<double[]> withNull = CameraCalibrationUtils.computeResidualErrors(actual(), modeled(),
                (ArrayList<Integer>) null);
        List<double[]> withEmpty =
                CameraCalibrationUtils.computeResidualErrors(actual(), modeled(), outliers());

        assertEquals(withEmpty.size(), withNull.size());
    }

    @Test
    public void raggedHeightsAreHandled() {
        // Calibration heights need not carry the same number of points.
        double[][][] actual = {{{10, 20}}, {{30, 40}, {50, 60}}};
        double[][][] modeled = {{{9, 19}}, {{28, 38}, {45, 55}}};

        List<double[]> residuals = CameraCalibrationUtils.computeResidualErrors(actual, modeled);

        assertEquals(3, residuals.size());
        assertArrayEquals2D(new double[] {1, 1}, residuals.get(0));
        assertArrayEquals2D(new double[] {2, 2}, residuals.get(1));
        assertArrayEquals2D(new double[] {5, 5}, residuals.get(2));
    }

    @Test
    public void negativeResidualsArePreserved() {
        double[][][] actual = {{{10, 20}}};
        double[][][] modeled = {{{12, 25}}};

        List<double[]> residuals = CameraCalibrationUtils.computeResidualErrors(actual, modeled);

        assertArrayEquals2D(new double[] {-2, -5}, residuals.get(0));
    }

    @Test
    public void drmsOfASinglePointIsItsDistance() {
        double drms = CameraCalibrationUtils.computeDrmsError(residuals(new double[] {3, 4}));

        assertEquals(5.0, drms, DELTA);
    }

    @Test
    public void drmsIsRootOfTheMeanSquaredDistance() {
        double drms = CameraCalibrationUtils
                .computeDrmsError(residuals(new double[] {3, 4}, new double[] {0, 0}));

        assertEquals(Math.sqrt(25.0 / 2), drms, DELTA);
    }

    @Test
    public void drmsIgnoresTheSignOfTheResiduals() {
        double positive = CameraCalibrationUtils.computeDrmsError(residuals(new double[] {3, 4}));
        double negative = CameraCalibrationUtils.computeDrmsError(residuals(new double[] {-3, -4}));

        assertEquals(positive, negative, DELTA);
    }

    @Test
    public void drmsOfPerfectFitIsZero() {
        double drms = CameraCalibrationUtils
                .computeDrmsError(residuals(new double[] {0, 0}, new double[] {0, 0}));

        assertEquals(0.0, drms, DELTA);
    }

    /** Dividing by a point count of zero, so the caller has to avoid this case itself. */
    @Test
    public void drmsOfAnEmptyListIsNotANumber() {
        double drms = CameraCalibrationUtils.computeDrmsError(new ArrayList<double[]>());

        assertTrue(Double.isNaN(drms));
    }

    @Test
    public void residualsFeedStraightIntoDrms() {
        List<double[]> residuals =
                CameraCalibrationUtils.computeResidualErrors(actual(), modeled());

        // (1,2) (3,4) (5,6) (7,8) -> (1+4 + 9+16 + 25+36 + 49+64) / 4
        assertEquals(Math.sqrt(204.0 / 4),
                CameraCalibrationUtils.computeDrmsError(residuals), DELTA);
    }

    private static List<double[]> residuals(double[]... points) {
        return new ArrayList<>(Arrays.asList(points));
    }

    private static void assertArrayEquals2D(double[] expected, double[] actual) {
        assertEquals(2, actual.length);
        assertEquals(expected[0], actual[0], DELTA, "x component");
        assertEquals(expected[1], actual[1], DELTA, "y component");
    }
}
