package utils;

import models.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import static models.Board.BOARD_DIMENSION;


/**
 * Component of the Chess game that detects check mates in the game.
 *
 * @author Jussi Lundstedt
 */
public class CheckmateDetector {
    private Board board;
    private LinkedList<Piece> whitePieces;
    private LinkedList<Piece> blackPieces;
    private LinkedList<Square> movableSquares;
    private final LinkedList<Square> squares;
    private King blackKing;
    private King whiteKing;
    private HashMap<Square, List<Piece>> whiteMoves;
    private HashMap<Square, List<Piece>> blackMoves;

    /**
     * Constructs a new instance of utils.CheckmateDetector on a given board. By
     * convention should be called when the board is in its initial state.
     *
     * @param board       The board which the detector monitors
     * @param whitePieces White pieces on the board.
     * @param blackPieces Black pieces on the board.
     * @param whiteKing   models.Piece object representing the white king
     * @param blackKing   models.Piece object representing the black king
     */
    public CheckmateDetector(Board board, LinkedList<Piece> whitePieces,
                             LinkedList<Piece> blackPieces, King whiteKing, King blackKing) {
        this.board = board;
        this.whitePieces = whitePieces;
        this.blackPieces = blackPieces;
        this.blackKing = blackKing;
        this.whiteKing = whiteKing;

        // Initialize other fields
        squares = new LinkedList<>();
        movableSquares = new LinkedList<>();
        whiteMoves = new HashMap<>();
        blackMoves = new HashMap<>();

        Square[][] gameBoard = board.getSquareArray();

        // add all squares to squares list and as hashmap keys
        for (int x = 0; x < BOARD_DIMENSION; x++) {
            for (int y = 0; y < BOARD_DIMENSION; y++) {
                squares.add(gameBoard[y][x]);
                whiteMoves.put(gameBoard[y][x], new LinkedList<>());
                blackMoves.put(gameBoard[y][x], new LinkedList<>());
            }
        }

        // update situation
        update();
    }

    /**
     * Updates the object with the current situation of the game.
     */
    public void update() {
        // Iterators through pieces
        Iterator<Piece> whiteIterator = whitePieces.iterator();
        Iterator<Piece> blackIterator = blackPieces.iterator();

        // empty moves and movable squares at each update
        for (List<Piece> pieces : whiteMoves.values()) {
            pieces.clear();
        }

        for (List<Piece> pieces : blackMoves.values()) {
            pieces.clear();
        }

        movableSquares.clear();

        // Add each canMove white and black can make to map
        while (whiteIterator.hasNext()) {
            Piece piece = whiteIterator.next();

            if (!piece.getClass().equals(King.class)) {
                if (piece.getPosition() == null) {
                    whiteIterator.remove();
                    continue;
                }

                List<Square> legalMoves = piece.getLegalMoves(board);
                for (Square legalMove : legalMoves) {
                    List<Piece> pieces = whiteMoves.get(legalMove);
                    pieces.add(piece);
                }
            }
        }

        while (blackIterator.hasNext()) {
            Piece piece = blackIterator.next();

            if (!piece.getClass().equals(King.class)) {
                if (piece.getPosition() == null) {
                    whiteIterator.remove();
                    continue;
                }

                List<Square> legalMoves = piece.getLegalMoves(board);
                for (Square legalMove : legalMoves) {
                    List<Piece> pieces = blackMoves.get(legalMove);
                    pieces.add(piece);
                }
            }
        }
    }

    /**
     * Checks if the black king is threatened
     *
     * @return boolean representing whether the black king is in check.
     */
    private boolean blackInCheck() {
        update();
        Square blackKingPosition = blackKing.getPosition();
        if (whiteMoves.get(blackKingPosition).isEmpty()) {
            movableSquares.addAll(squares);
            return false;
        } else return true;
    }

    /**
     * Checks if the white king is threatened
     *
     * @return boolean representing whether the white king is in check.
     */
    private boolean whiteInCheck() {
        update();
        Square whiteKingPosition = whiteKing.getPosition();
        if (blackMoves.get(whiteKingPosition).isEmpty()) {
            movableSquares.addAll(squares);
            return false;
        } else return true;
    }

    /**
     * Checks whether black is in checkmate.
     *
     * @return boolean representing if black player is checkmated.
     */
    public boolean blackCheckMated() {
        boolean isCheckmate = true;
        // Check if black is in check
        if (!this.blackInCheck()) return false;

        // If yes, check if king can evade
        if (canEvade(whiteMoves, blackKing)) isCheckmate = false;

        // If no, check if threat can be captured
        List<Piece> threats = whiteMoves.get(blackKing.getPosition());
        if (canCapture(blackMoves, threats, blackKing)) isCheckmate = false;

        // If no, check if threat can be blocked
        if (canBlock(threats, blackMoves, blackKing)) isCheckmate = false;

        // If no possible ways of removing check, isCheckmate occurred
        return isCheckmate;
    }

    /**
     * Checks whether white is in checkmate.
     *
     * @return boolean representing if white player is checkmated.
     */
    public boolean whiteCheckMated() {
        boolean isCheckmate = true;
        // Check if white is in check
        if (!this.whiteInCheck()) return false;

        // If yes, check if king can evade
        if (canEvade(blackMoves, whiteKing)) isCheckmate = false;

        // If no, check if threat can be captured
        List<Piece> threats = blackMoves.get(whiteKing.getPosition());
        if (canCapture(whiteMoves, threats, whiteKing)) isCheckmate = false;

        // If no, check if threat can be blocked
        if (canBlock(threats, whiteMoves, whiteKing)) isCheckmate = false;

        // If no possible ways of removing check, isCheckmate occurred
        return isCheckmate;
    }

    /*
     * Helper method to determine if the king can evade the check.
     * Gives a false positive if the king can capture the checking piece.
     */
    private boolean canEvade(Map<Square, List<Piece>> blockMoves, King king) {
        boolean canEvade = false;
        List<Square> kingsMoves = king.getLegalMoves(board);

        // If king is not threatened at some square, it can canEvade
        for (Square square : kingsMoves) {
            if (!testMove(king, square)) continue;
            if (blockMoves.get(square).isEmpty()) {
                movableSquares.add(square);
                canEvade = true;
            }
        }

        return canEvade;
    }

    /*
     * Helper method to determine if the threatening piece can be captured.
     */
    private boolean canCapture(Map<Square, List<Piece>> positions,
                               List<Piece> threats, King king) {

        boolean canCapture = false;
        if (threats.size() == 1) {
            Square position = threats.get(0).getPosition();

            if (king.getLegalMoves(board).contains(position)) {
                movableSquares.add(position);
                if (testMove(king, position)) {
                    canCapture = true;
                }
            }

            List<Piece> captures = positions.get(position);
            ConcurrentLinkedDeque<Piece> capturers = new ConcurrentLinkedDeque<>(captures);

            if (!capturers.isEmpty()) {
                movableSquares.add(position);
                for (Piece piece : capturers) {
                    if (testMove(piece, position)) {
                        canCapture = true;
                    }
                }
            }
        }

        return canCapture;
    }

    /*
     * Helper method to determine if check can be blocked by a piece.
     */
    private boolean canBlock(List<Piece> threats,
                             Map<Square, List<Piece>> blockMoves, King king) {
        boolean blockable = false;

        if (threats.size() == 1) {
            Square position = threats.get(0).getPosition();
            Square kingPosition = king.getPosition();
            Square[][] brdArray = board.getSquareArray();

            if (kingPosition.getXNum() == position.getXNum()) {
                int max = Math.max(kingPosition.getYNum(), position.getYNum());
                int min = Math.min(kingPosition.getYNum(), position.getYNum());

                for (int i = min + 1; i < max; i++) {
                    List<Piece> blocks =
                            blockMoves.get(brdArray[i][kingPosition.getXNum()]);
                    ConcurrentLinkedDeque<Piece> blockers =
                            new ConcurrentLinkedDeque<>(blocks);

                    if (!blockers.isEmpty()) {
                        movableSquares.add(brdArray[i][kingPosition.getXNum()]);

                        for (Piece piece : blockers) {
                            if (testMove(piece, brdArray[i][kingPosition.getXNum()])) {
                                blockable = true;
                            }
                        }

                    }
                }
            }

            if (kingPosition.getYNum() == position.getYNum()) {
                int max = Math.max(kingPosition.getXNum(), position.getXNum());
                int min = Math.min(kingPosition.getXNum(), position.getXNum());

                for (int i = min + 1; i < max; i++) {
                    List<Piece> blocks =
                            blockMoves.get(brdArray[kingPosition.getYNum()][i]);
                    ConcurrentLinkedDeque<Piece> blockers =
                            new ConcurrentLinkedDeque<>(blocks);

                    if (!blockers.isEmpty()) {

                        movableSquares.add(brdArray[kingPosition.getYNum()][i]);

                        for (Piece p : blockers) {
                            if (testMove(p, brdArray[kingPosition.getYNum()][i])) {
                                blockable = true;
                            }
                        }

                    }
                }
            }

            Class<? extends Piece> tC = threats.get(0).getClass();

            if (tC.equals(Queen.class) || tC.equals(Bishop.class)) {
                int kX = kingPosition.getXNum();
                int kY = kingPosition.getYNum();

                int tX = position.getXNum();
                int tY = position.getYNum();

                if (kX > tX && kY > tY) {
                    for (int i = tX + 1; i < kX; i++) {
                        tY++;
                        List<Piece> blocks =
                                blockMoves.get(brdArray[tY][i]);
                        ConcurrentLinkedDeque<Piece> blockers =
                                new ConcurrentLinkedDeque<>(blocks);

                        if (!blockers.isEmpty()) {
                            movableSquares.add(brdArray[tY][i]);

                            for (Piece piece : blockers) {
                                if (testMove(piece, brdArray[tY][i])) {
                                    blockable = true;
                                }
                            }
                        }
                    }
                }

                if (kX > tX && tY > kY) {
                    for (int i = tX + 1; i < kX; i++) {
                        tY--;
                        List<Piece> blocks =
                                blockMoves.get(brdArray[tY][i]);
                        ConcurrentLinkedDeque<Piece> blockers =
                                new ConcurrentLinkedDeque<>(blocks);

                        if (!blockers.isEmpty()) {
                            movableSquares.add(brdArray[tY][i]);

                            for (Piece piece : blockers) {
                                if (testMove(piece, brdArray[tY][i])) {
                                    blockable = true;
                                }
                            }
                        }
                    }
                }

                if (tX > kX && kY > tY) {
                    for (int i = tX - 1; i > kX; i--) {
                        tY++;
                        List<Piece> blocks =
                                blockMoves.get(brdArray[tY][i]);
                        ConcurrentLinkedDeque<Piece> blockers =
                                new ConcurrentLinkedDeque<>(blocks);

                        if (!blockers.isEmpty()) {
                            movableSquares.add(brdArray[tY][i]);

                            for (Piece piece : blockers) {
                                if (testMove(piece, brdArray[tY][i])) {
                                    blockable = true;
                                }
                            }
                        }
                    }
                }

                if (tX > kX && tY > kY) {
                    for (int i = tX - 1; i > kX; i--) {
                        tY--;
                        List<Piece> blocks =
                                blockMoves.get(brdArray[tY][i]);
                        ConcurrentLinkedDeque<Piece> blockers =
                                new ConcurrentLinkedDeque<>(blocks);

                        if (!blockers.isEmpty()) {
                            movableSquares.add(brdArray[tY][i]);

                            for (Piece piece : blockers) {
                                if (testMove(piece, brdArray[tY][i])) {
                                    blockable = true;
                                }
                            }
                        }
                    }
                }
            }
        }

        return blockable;
    }

    /**
     * Method to get a list of allowable squares that the player can canMove.
     * Defaults to all squares, but limits available squares if player is in
     * check.
     *
     * @return List of squares that the player can canMove into.
     */
    public List<Square> getAllowableSquares() {
        movableSquares.clear();
        if (whiteInCheck()) {
            whiteCheckMated();
        } else if (blackInCheck()) {
            blackCheckMated();
        }
        return movableSquares;
    }

    /**
     * Tests a canMove a player is about to make to prevent making an illegal canMove
     * that puts the player in check.
     *
     * @param piece  models.Piece moved
     * @param square models.Square to which piece is about to canMove
     * @return false if canMove would cause a check
     */
    public boolean testMove(Piece piece, Square square) {
        Piece c = square.getOccupyingPiece();

        boolean moveTest = true;
        Square initialSquare = piece.getPosition();

        piece.canMove(square);
        update();

        if (piece.getColor() == 0 && blackInCheck()) moveTest = false;
        else if (piece.getColor() == 1 && whiteInCheck()) moveTest = false;

        piece.canMove(initialSquare);
        if (c != null) square.put(c);

        update();

        movableSquares.addAll(squares);
        return moveTest;
    }

}
