/*
 * Created on Nov 18, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.pb.despair.calibrator;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 * @author jabraham
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class FieldDocumentListener implements DocumentListener {

    static interface FieldSetter {
        public void setField(String f);
    }
    
    FieldSetter myFieldSetter;
    public FieldDocumentListener(FieldSetter r) {
        myFieldSetter = r;
    }

    public void changedUpdate(DocumentEvent arg0) {
        changeValue(arg0);
    }
    public void insertUpdate(DocumentEvent arg0) {
        changeValue(arg0);
    }
    public void removeUpdate(DocumentEvent arg0) {
        changeValue(arg0);
    }
    private void changeValue(DocumentEvent arg0) {
        Document doc = arg0.getDocument();
        String text = null;
        try {
            text = doc.getText(doc.getStartPosition().getOffset(), doc.getLength());
            myFieldSetter.setField(text);
        } catch (NumberFormatException e) {
            // jsut don't change it until user enters correct value
        } catch (BadLocationException e1) {
            e1.printStackTrace();
            text = "";
        }
    }
}
