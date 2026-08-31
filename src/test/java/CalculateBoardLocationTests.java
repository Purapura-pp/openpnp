import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.openpnp.model.Board;
import org.openpnp.model.Abstract2DLocatable.Side;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Placement;
import org.openpnp.util.Utils2D;

public class CalculateBoardLocationTests {
    @Test
    public void calculateBoardLocationTopNoAffineNoWidth() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Top, false);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);
        
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        
        Utils2DTest.checkNormalized(p1l, 23.160, 130.902, -10.00, 119.628);
    }
    
    @Test
    public void calculateBoardLocationTopWithAffineNoWidth() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Top, false);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);

        AffineTransform tx = simulateFiducialCheck(boardLocation);
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 0, 0, -10, 0));
        boardLocation.setPlacementTransform(tx);
        
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        
        Utils2DTest.checkNormalized(p1l, 23.160, 130.902, -10.00, 119.628);
    }
    
    @Test
    public void calculateBoardLocationTopNoAffineWithWidth() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Top, true);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);

        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        
        Utils2DTest.checkNormalized(p1l, 23.160, 130.902, -10.00, 119.628);
    }
    
    @Test
    public void calculateBoardLocationTopWithAffineWithWidth() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Top, true);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);

        AffineTransform tx = simulateFiducialCheck(boardLocation);
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 0, 0, -10, 0));
        boardLocation.setPlacementTransform(tx);
        
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        
        Utils2DTest.checkNormalized(p1l, 23.160, 130.902, -10.00, 119.628);
    }
    
    @Test
    public void calculateBoardLocationBottomNoAffineNoWidth() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Bottom, false);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);

        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        
        Utils2DTest.checkNormalized(p1l, 91.015, 79.253, -10.00, 81.585);
    }
    
    @Test
    public void calculateBoardLocationBottomWithAffineNoWidth() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Bottom, false);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);

        AffineTransform tx = simulateFiducialCheck(boardLocation);
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 0, 0, -10, 0));
        boardLocation.setPlacementTransform(tx);
        
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        
        Utils2DTest.checkNormalized(p1l, 91.015, 79.253, -10.00, 81.585);
    }
    
    @Test
    public void calculateBoardLocationBottomNoAffineWithWidth() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Bottom, true);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);
        
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        
        Utils2DTest.checkNormalized(p1l, 91.025, 79.246, -10.00, 81.662);
    }
    
    @Test
    public void calculateBoardLocationBottomWithAffineWithWidth() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Bottom, true);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);

        AffineTransform tx = simulateFiducialCheck(boardLocation);
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 0, 0, -10, 0));
        boardLocation.setPlacementTransform(tx);
        
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        
        Utils2DTest.checkNormalized(p1l, 91.025, 79.246, -10.00, 81.662);
    }
    
    @Test
    public void calculateBoardLocationInverseTopNoAffineNoWidth() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Top, false);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);
        
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        
        Location p1li = Utils2D.calculateBoardPlacementLocationInverse(boardLocation, p1l);
        
        Utils2DTest.checkNormalized(p1.getLocation(), p1li);
    }
    
    @Test
    public void calculateBoardLocationInverseTopWithAffineNoWidth() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Top, false);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);
        
        AffineTransform tx = simulateFiducialCheck(boardLocation);
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 0, 0, -10, 0));
        boardLocation.setPlacementTransform(tx);
        
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        
        Location p1li = Utils2D.calculateBoardPlacementLocationInverse(boardLocation, p1l);
        
        Utils2DTest.checkNormalized(p1.getLocation(), p1li);
    }
    
    @Test
    public void calculateBoardLocationInverseTopNoAffineWithWidth() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Top, true);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);
        
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        
        Location p1li = Utils2D.calculateBoardPlacementLocationInverse(boardLocation, p1l);
        
        Utils2DTest.checkNormalized(p1.getLocation(), p1li);
    }
    
    @Test
    public void calculateBoardLocationInverseTopWithAffineWithWidth() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Top, true);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);
        
        AffineTransform tx = simulateFiducialCheck(boardLocation);
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 0, 0, -10, 0));
        boardLocation.setPlacementTransform(tx);
        
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        
        Location p1li = Utils2D.calculateBoardPlacementLocationInverse(boardLocation, p1l);
        
        Utils2DTest.checkNormalized(p1.getLocation(), p1li);
    }
    
    @Test
    public void calculateBoardLocationInverseBottomNoAffineNoWidth() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Bottom, false);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);
        
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        
        Location p1li = Utils2D.calculateBoardPlacementLocationInverse(boardLocation, p1l);
        
        Utils2DTest.checkNormalized(p1.getLocation(), p1li);
    }
    
    @Test
    public void calculateBoardLocationInverseBottomWithAffineNoWidth() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Bottom, false);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);
        
        AffineTransform tx = simulateFiducialCheck(boardLocation);
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 0, 0, -10, 0));
        boardLocation.setPlacementTransform(tx);
        
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        
        Location p1li = Utils2D.calculateBoardPlacementLocationInverse(boardLocation, p1l);
        
        Utils2DTest.checkNormalized(p1.getLocation(), p1li);
    }
    
    @Test
    public void calculateBoardLocationInverseBottomNoAffineWithWidth() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Bottom, true);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);
        
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        
        Location p1li = Utils2D.calculateBoardPlacementLocationInverse(boardLocation, p1l);
        
        Utils2DTest.checkNormalized(p1.getLocation(), p1li);
    }
    
    @Test
    public void calculateBoardLocationInverseBottomWithAffineWithWidth() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Bottom, true);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);
        
        AffineTransform tx = simulateFiducialCheck(boardLocation);
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 0, 0, -10, 0));
        boardLocation.setPlacementTransform(tx);
        
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        
        Location p1li = Utils2D.calculateBoardPlacementLocationInverse(boardLocation, p1l);

        Utils2DTest.checkNormalized(p1.getLocation(), p1li);
    }

    /**
     * A transform derived from degenerate fiducial measurements has no inverse, so there is no
     * machine to placement mapping to compute at all. Refusing is deliberate: the calculation used
     * to carry on with the uninverted transform, which returned a plausible looking but wrong
     * coordinate.
     */
    @Test
    public void invertingADegenerateTransformIsRefused() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Top, false);
        // Both basis vectors point the same way, collapsing the board frame onto a line.
        boardLocation.setPlacementTransform(new AffineTransform(1, 1, 1, 1, 0, 0));

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> Utils2D.calculateBoardPlacementLocationInverse(boardLocation,
                        new Location(LengthUnit.Millimeters, 10, 20, 0, 0)));

        assertTrue(e.getMessage().contains("degenerate"), e.getMessage());
    }

    /** The forward direction needs no inverse, so it still answers for a degenerate transform. */
    @Test
    public void theForwardCalculationStillWorksWithADegenerateTransform() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Top, false);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);
        boardLocation.setPlacementTransform(new AffineTransform(1, 1, 1, 1, 0, 0));

        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());

        assertTrue(Double.isFinite(p1l.getX()));
        assertTrue(Double.isFinite(p1l.getY()));
    }




    
    /**
     * Simulates a 3 point fiducial check by generating 3 placements at fixed locations,
     * calculating their board placement location and running deriveAffineTransform.
     * 
     * This is to be used when it is known that calculateBoardLocation is working correctly
     * for the given BoardLocation without a placement transform set.
     * 
     * @param boardLocation
     * @return
     */
    private AffineTransform simulateFiducialCheck(BoardLocation boardLocation) {
        Placement fid1 = new Placement("FID1");
        fid1.setLocation(new Location(LengthUnit.Millimeters, 1, 1, 0, 0));
        
        Placement fid2 = new Placement("FID2");
        fid2.setLocation(new Location(LengthUnit.Millimeters, 2, 1, 0, 0));
        
        Placement fid3 = new Placement("FID3");
        fid3.setLocation(new Location(LengthUnit.Millimeters, 2, 2, 0, 0));
        
        boardLocation.setPlacementTransform(null);
        List<Location> globalLocations = new ArrayList<>();
        globalLocations.add(Utils2D.calculateBoardPlacementLocation(boardLocation, fid1.getLocation()));
        globalLocations.add(Utils2D.calculateBoardPlacementLocation(boardLocation, fid2.getLocation()));
        globalLocations.add(Utils2D.calculateBoardPlacementLocation(boardLocation, fid3.getLocation()));
        
        //Utils2D.deriveAffineTransform expects to be using a right-handed coordinate system; but,
        //the board's bottom coordinate system is left-handed so we need to change the sign of all 
        //the x components before computing the Affine Transform and then change the sign of its 
        //x scaling afterwards.
        if (boardLocation.getGlobalSide() == Side.Bottom) {
            fid1.setLocation(fid1.getLocation().multiply(-1, 1, 1, 1));
            fid2.setLocation(fid2.getLocation().multiply(-1, 1, 1, 1));
            fid3.setLocation(fid3.getLocation().multiply(-1, 1, 1, 1));
        }
        
        List<Location> localLocations = new ArrayList<>();
        localLocations.add(fid1.getLocation());
        localLocations.add(fid2.getLocation());
        localLocations.add(fid3.getLocation());

        AffineTransform tx = Utils2D.deriveAffineTransform(localLocations, globalLocations);
        
        if (boardLocation.getGlobalSide() == Side.Bottom) {
            tx.scale(-1, 1);
        }
        
        boardLocation.setPlacementTransform(tx);
        
        return tx;
    }
    
    /**
     * These match the pnp-test setup with simulation. The two bottom-side board locations were
     * measured separately on the machine - one with the origin at the corner the board width is
     * measured from, one at the opposite corner - so each carries its own jog error, and the
     * expected results for the two cases differ slightly as a result.
     * <p>
     * That difference is in the inputs, not in the transform. Feeding the without-width case the
     * location the with-width case implies, 37 mm along the recorded 36.662 deg, gives
     * (113.787, 68.763) and both cases then produce byte-identical results: the bottom-side
     * transform is symmetric in board width.
     * <p>
     * The recorded locations are 36.9665 mm apart, 0.0335 mm short of the nominal 37, and their
     * recorded rotations differ by 0.077 deg. Re-measuring the board width does not reconcile
     * them - substituting that 36.9665, or the 37.06 an earlier note guessed at, widens the gap
     * between the two cases from 0.0125 mm to 0.0319 mm and 0.0651 mm respectively. The dominant
     * term is the 0.077 deg, which is roughly 0.05 mm of arc at this distance from the origin and
     * cannot be absorbed into a width. Nominal 37 is the closest of the candidates, which is why
     * it is what the tests use.
     */
    static BoardLocation createTestBoardLocation(Side side, boolean includeBoardWidth) {
        Board board = new Board();
        if (includeBoardWidth) {
            board.setDimensions(new Location(LengthUnit.Millimeters, 37.0, 0, 0, 0));
        }
        
        BoardLocation boardLocation = new BoardLocation(board);
        boardLocation.setGlobalSide(side);
        if (side == Side.Top) {
            Placement r6 = new Placement("R6");
            r6.setLocation(new Location(LengthUnit.Millimeters, 25, 22, 0, 45));
            r6.setSide(side);
            board.addPlacement(r6);
            
            boardLocation.setLocation(new Location(LengthUnit.Millimeters, 37.746, 100.964, -10, 74.628));
        }
        else {
            Placement r17 = new Placement("R17");
            r17.setLocation(new Location(LengthUnit.Millimeters, 12, 22, 0, 45));
            r17.setSide(side);
            board.addPlacement(r17);

            if (includeBoardWidth) {
                boardLocation.setLocation(new Location(LengthUnit.Millimeters, 84.107, 46.671, -10, 36.662));
            }
            else {
                boardLocation.setLocation(new Location(LengthUnit.Millimeters, 113.763, 68.740, -10, 36.585));
            }
        }
        
        return boardLocation;
    }
}
