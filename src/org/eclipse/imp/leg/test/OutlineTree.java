/**
 * 
 */
package org.eclipse.imp.leg.test;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

public class OutlineTree {
    private String fText;
    private OutlineTree[] fChildren;
    public OutlineTree(String text) {
        this.fText= text;
    }
    public OutlineTree(String text, OutlineTree[] children) {
        this.fText= text;
        this.fChildren= children;
    }
    public void checkStructure(SWTBotTreeItem actualItem) {
        junit.framework.Assert.assertEquals("Tree item has wrong text", fText, actualItem.getText());

        actualItem.expand();
        checkChildStructure(actualItem.getItems());
    }
    public void checkChildStructure(SWTBotTreeItem[] actualItems) {
        if (actualItems == null && fChildren == null) {
            return;
        }
        junit.framework.Assert.assertTrue((fChildren == null && actualItems.length == 0) || (actualItems.length == fChildren.length));
        for(int i=0; i < actualItems.length; i++) {
            SWTBotTreeItem actualItem= actualItems[i];
            OutlineTree expected= fChildren[i];

            expected.checkStructure(actualItem);
        }
    }
}