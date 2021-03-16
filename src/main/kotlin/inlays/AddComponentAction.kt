package inlays

import com.intellij.icons.AllIcons
import com.intellij.ide.highlighter.HighlighterFactory
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.FileContentUtil
import net.miginfocom.swing.MigLayout
import javax.swing.JButton
import javax.swing.JPanel

/**
 * @author Colin Fleming
 */
class AddComponentAction : AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = true
        e.presentation.isVisible = true
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(PlatformDataKeys.EDITOR) ?: return
        val project = e.getData(PlatformDataKeys.PROJECT) ?: return
        val manager = EditorComponentInlaysManager.from(editor)
        val lineNumber = editor.document.getLineNumber(editor.caretModel.offset)
        val inlayRef = Ref<Disposable>()
        val panel = makePanel(makeEditor(project), inlayRef)
        val inlay = manager.insertAfter(lineNumber, panel)
        inlayRef.set(inlay)
    }

    fun makePanel(editor: Editor, inlayRef: Ref<Disposable>): JPanel {
        val action = object : AnAction({ "Close" }, AllIcons.Actions.Close) {
            override fun actionPerformed(e: AnActionEvent) {
                inlayRef.get().dispose()
            }
        }

        val closeButton = ActionButton(
            action,
            action.templatePresentation.clone(),
            ActionPlaces.TOOLBAR,
            ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
        )

        val toolbarPanel = JPanel(MigLayout("insets 0, gap 0!, fillx")).apply {
            add(JButton("Click Me"))
            add(closeButton, "gapbefore 20:push")
        }

        return JPanel(MigLayout("wrap 1, insets 0, gap 0!, fillx")).apply {
            add(toolbarPanel, "growx, growy 0, gap 0!")
            add(editor.contentComponent, "grow, gap 0!")
        }
    }

    fun makeEditor(project: Project): Editor {
        val factory = EditorFactory.getInstance()
        val text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod\n" +
                "tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam,\n" +
                "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo\n" +
                "consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse\n" +
                "cillum dolore eu fugiat nulla pariatur."
        val document = factory.createDocument(text)
        val editor = factory.createEditor(document, project)
        val virtualFile = LightVirtualFile("ipsum.txt", PlainTextLanguage.INSTANCE, text)
        virtualFile.setContent(document, document.text, false)
        FileContentUtil.reparseFiles(project, listOf(virtualFile), false)
        (editor as? EditorEx)?.highlighter = HighlighterFactory.createHighlighter(project, PlainTextFileType.INSTANCE)
        editor.setBorder(null)
        (editor as? EditorEx)?.scrollPane?.viewportBorder = JBScrollPane.createIndentBorder()
        return editor
    }
}
