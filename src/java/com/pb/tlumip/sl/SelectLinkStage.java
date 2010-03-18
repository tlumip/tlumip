package com.pb.tlumip.sl;

/**
 * @author crf <br/>
 *         Started: Nov 20, 2009 4:27:20 PM
 */
public enum SelectLinkStage {
    //the order here is the order these should be run in
    GENERATE_PATHS('g'),
    GENERATE_SELECT_LINK_DATA('d'),
    CREATE_SUBAREA_MATRIX('s');


    private final char stageChar;
    private SelectLinkStage(char stageChar) {
        this.stageChar = stageChar;
    }

    public char getStageChar() {
        return stageChar;
    }

    public static boolean isValidStageChar(char stageChar) {
        for (SelectLinkStage stage : SelectLinkStage.values())
            if (stage.stageChar == stageChar)
                return true;
        return false;
    }
}
