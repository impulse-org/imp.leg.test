package org.eclipse.imp.leg.test;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.waits.Conditions;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEclipseEditor;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.results.Result;
import org.eclipse.swtbot.swt.finder.utils.Position;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SWTBotJunit4ClassRunner.class)
public class Test1 {
    private static SWTWorkbenchBot bot;

    final static String lineSep= System.getProperty("line.separator");

    private static Color commentColor; // should match what the LEG token colorer produces
    private static Color numberColor; // should match what the LEG token colorer produces
    private static Color keywordColor; // should match what the LEG token colorer produces
    private static Color identifierColor; // should match what the LEG token colorer produces

    private int commentStyle = SWT.ITALIC;
    private int identifierStyle = SWT.NORMAL;
    private int numberStyle = SWT.BOLD;
    private int keywordStyle = SWT.BOLD;

    @BeforeClass
    public static void beforeClass() throws Exception {
        bot= new SWTWorkbenchBot();
        // Close the "Welcome" page
        bot.viewByTitle("Welcome").close();
        collectSystemColors();
        createPlainProject("LEGTest");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        // Among other things, the following makes sure that the workbench (and therefore the test)
        // shuts down cleanly, even if there are "dirty" open editors. Without it, the test might
        // hang waiting for someone to dismiss the "Foo has been modified. Save changes?" dialog.
        resetWorkbench();
    }

    @Test
    public void checkOutline() {
        final String fileName= "outline.leg";
        createPlainFile(fileName);

        final SWTBotEclipseEditor srcEditor= bot.editorByTitle(fileName).toTextEditor();

        typeIntoEditor(srcEditor,
                "int main() {" + lineSep +
                "    int x= 5;" + lineSep +
                "    int y= 125;" + lineSep +
                "    return x*y;" + lineSep +
                "}" + lineSep +
                "    // a comment" + lineSep +
                "int bar() {" + lineSep +
                "    int a= 51;" + lineSep +
                "    int b= 129;" + lineSep +
                "    return a+b;" + lineSep +
                "}" + lineSep);

        srcEditor.save();

        bot.sleep(500); // so syntax highlighting can catch up

        OutlineTree expectedTree= new OutlineTree("", new OutlineTree[] {
                new OutlineTree("int main()", new OutlineTree[] {
                        new OutlineTree("Block", new OutlineTree[] {
                                new OutlineTree("int x"), new OutlineTree("int y")
                        })}),
                new OutlineTree("int bar()", new OutlineTree[] {
                    new OutlineTree("Block", new OutlineTree[] {
                            new OutlineTree("int a"), new OutlineTree("int b")
                    })}),
                });
        SWTBotView outlineView= bot.viewByTitle("Outline");
        SWTBotTree outlineTree= outlineView.bot().tree();
        SWTBotTreeItem[] allItems= outlineTree.getAllItems();

        // Check that the outline has the right contents
        expectedTree.checkChildStructure(allItems);

        // Now check that clicking on outline items moves the editor's cursor to the right spot.
        outlineTree.select("int main()");
        junit.framework.Assert.assertTrue(srcEditor.getSelection().startsWith("int main()"));
        Position pos= srcEditor.cursorPosition();
        junit.framework.Assert.assertEquals("Selecting outline item did not move editor cursor to proper line", 0, pos.line);
        junit.framework.Assert.assertEquals("Selecting outline item did not move editor cursor to proper column", 0, pos.column);

        outlineTree.select("int bar()");
        junit.framework.Assert.assertTrue(srcEditor.getSelection().startsWith("int bar()"));
        Position pos1= srcEditor.cursorPosition();
        junit.framework.Assert.assertEquals("Selecting outline item did not move editor cursor to proper line", 6, pos1.line);
        junit.framework.Assert.assertEquals("Selecting outline item did not move editor cursor to proper column", 0, pos1.column);

        // Following doesn't work... why?
//        outlineTree.select("int x");
//        junit.framework.Assert.assertTrue(srcEditor.getSelection().startsWith("int x"));
//        outlineTree.select("int a");
//        junit.framework.Assert.assertTrue(srcEditor.getSelection().startsWith("int a"));

        // Following doesn't work... why?
        // Make a modification w/o saving and check that the outline updates
//        appendToEditor(srcEditor, "int bletch() { return 3; }");
//        bot.sleep(500);
//        SWTBotTreeItem[] allItemsNew= outlineTree.getAllItems();
//        junit.framework.Assert.assertEquals(3, allItemsNew.length);
//        junit.framework.Assert.assertEquals("int bletch()", allItemsNew[2].getText());
    }

    @Test
    public void checkSyntaxColoring() {
        final String fileName= "syntax.leg";
        createPlainFile(fileName);

        final SWTBotEclipseEditor srcEditor= bot.editorByTitle(fileName).toTextEditor();

        typeIntoEditor(srcEditor,
                "void main() {" + lineSep +
                "    int x= 5;" + lineSep +
                "    int y= 125;" + lineSep +
                "    return x*y;" + lineSep +
                "}" + lineSep +
                "    // a comment" + lineSep +
                "void bar() {" + lineSep +
                "    int a= 51;" + lineSep +
                "    int b= 129;" + lineSep +
                "    return a+b;" + lineSep +
                "}" + lineSep);

        srcEditor.save();

        bot.sleep(500); // so syntax highlighting can catch up

        checkColoringOfRange(srcEditor.getStyle(0, 2), keywordStyle, keywordColor);
        checkColoringOfRange(srcEditor.getStyle(5, 7), commentStyle, commentColor);
        checkColoringOfRange(srcEditor.getStyle(2, 12), numberStyle, numberColor);
        checkColoringOfRange(srcEditor.getStyle(0, 6), identifierStyle, identifierColor);
    }

    private void checkColoringOfRange(StyleRange styleRange, int style, Color color) {
        junit.framework.Assert.assertEquals(styleRange.fontStyle, style);
        junit.framework.Assert.assertEquals(styleRange.foreground, color);
    }

    private static void collectSystemColors() {
        commentColor= getSystemColor(SWT.COLOR_GREEN);
        numberColor= getSystemColor(SWT.COLOR_DARK_YELLOW);
        keywordColor= getSystemColor(SWT.COLOR_DARK_MAGENTA);
        identifierColor= getSystemColor(SWT.COLOR_BLACK);
    }

    private static Color getSystemColor(final int id) {
        return UIThreadRunnable.syncExec(bot.getDisplay(), new Result<Color>() {
            public Color run() {
                return PlatformUI.getWorkbench().getDisplay().getSystemColor(id);
            }
        });
    }

    private void appendToFile(String fileName, String contents) {
        final SWTBotEclipseEditor srcEditor= bot.editorByTitle(fileName).toTextEditor();

        appendToEditor(srcEditor, contents);
    }

    private void appendToEditor(final SWTBotEclipseEditor srcEditor, String text) {
        // srcEditor.selectRange(srcEditor.getLineCount(), 0, 0);
        srcEditor.navigateTo(srcEditor.getLineCount(), 0);
        srcEditor.insertText(/*srcEditor.getLineCount(), 0,*/ text);
    }

    private void typeIntoFile(String fileName, String contents) {
        final SWTBotEclipseEditor srcEditor= bot.editorByTitle(fileName).toTextEditor();

        typeIntoEditor(srcEditor, contents);
    }

    private void typeIntoEditor(final SWTBotEclipseEditor srcEditor, String contents) {
        srcEditor.insertText(contents);
    }

    private void createPlainFile(String fileName) {
        bot.menu("File").menu("New").menu("File").click();

        SWTBotShell newFileShell= bot.shell("New File");
        newFileShell.activate();
        newFileShell.bot().textWithLabel("File name:").setText(fileName);
        bot.button("Finish").click();

        bot.waitUntil(Conditions.shellCloses(newFileShell));
    }

    public static void createPlainProject(String name) {
        // create a project
        bot.menu("File").menu("New").menu("Project...").click();

        SWTBotShell newProjShell= bot.shell("New Project");

        // See my blog entry on the use of tree():
        // http://orquesta.watson.ibm.com/bblog/?p=117
        newProjShell.activate();
        newProjShell.bot().tree().expandNode("General").select("Project");
        bot.button("Next >").click();

        bot.textWithLabel("Project name:").setText(name);
        bot.button("Finish").click();

        bot.waitUntil(Conditions.shellCloses(newProjShell));
    }

    public static void resetWorkbench() {
        closeAllShells();
        saveAllEditors();
        closeAllEditors();
    }

    public static void closeAllShells() {
        SWTBotShell[] shells= bot.shells();
        for(SWTBotShell shell : shells) {
            if (!isEclipseShell(shell)) {
                shell.close();
            }
        }
    }

    public static void saveAllEditors() {
        List<? extends SWTBotEditor> editors= bot.editors();
        for(SWTBotEditor editor : editors) {
            editor.save();
        }
    }

    public static void closeAllEditors() {
        List<? extends SWTBotEditor> editors= bot.editors();
        for(SWTBotEditor editor : editors) {
            editor.close();
        }
    }

    private static boolean isEclipseShell(final SWTBotShell shell) {
        return getActiveWorkbenchWindowShell() == shell.widget;
    }

    private static IWorkbenchWindow getActiveWorkbenchWindow() {
        return UIThreadRunnable.syncExec(bot.getDisplay(), new Result<IWorkbenchWindow>() {
            public IWorkbenchWindow run() {
                return PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            }
        });
    }

    private static Widget getActiveWorkbenchWindowShell() {
        return getActiveWorkbenchWindow().getShell();
    }
}
