package inlays

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.actions.IncrementalFindAction
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.ui.EditorTextField
import com.intellij.util.ui.UIUtil
import net.miginfocom.swing.MigLayout
import java.awt.event.ComponentEvent
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
        panel.revalidate()
        inlayRef.set(inlay)
        val viewport = (editor as? EditorImpl)?.scrollPane?.viewport
        viewport?.dispatchEvent(ComponentEvent(viewport, ComponentEvent.COMPONENT_RESIZED))
    }

    fun makePanel(editor: EditorTextField, inlayRef: Ref<Disposable>): JPanel {
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
            add(editor, "grow, gap 0!")
        }
    }

    fun makeEditor(project: Project): EditorTextField {
        val factory = EditorFactory.getInstance()
        val text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod\n" +
                "tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam,\n" +
                "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo\n" +
                "consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse\n" +
                "cillum dolore eu fugiat nulla pariatur."
        val document = factory.createDocument(text)

        return object : EditorTextField(document, project, FileTypes.PLAIN_TEXT) {
            //always paint pretty border
            override fun updateBorder(editor: EditorEx) = setupBorder(editor)

            override fun createEditor(): EditorEx {
                // otherwise border background is painted from multiple places
                return super.createEditor().apply {
                    //TODO: fix in editor
                    //com.intellij.openapi.editor.impl.EditorImpl.getComponent() == non-opaque JPanel
                    // which uses default panel color
                    component.isOpaque = false
                    //com.intellij.ide.ui.laf.darcula.ui.DarculaEditorTextFieldBorder.paintBorder
                    scrollPane.isOpaque = false
                }
            }
        }.apply {
            putClientProperty(UIUtil.HIDE_EDITOR_FROM_DATA_CONTEXT_PROPERTY, true)
            setOneLineMode(false)
            setPlaceholder(text)
            addSettingsProvider {
                it.putUserData(IncrementalFindAction.SEARCH_DISABLED, true)
                it.colorsScheme.lineSpacing = 1f
                it.settings.isUseSoftWraps = true
            }
            selectAll()
        }
    }
}
