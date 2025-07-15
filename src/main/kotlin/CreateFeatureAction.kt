package com.mirkamalg

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class CreateFeatureAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        println("CreateFeatureAction.actionPerformed() called!")
        val project = e.project ?: return
        val projectRoot = project.baseDir ?: run {
            Messages.showErrorDialog("Could not find project root directory", "Error")
            return
        }

        val dialog = CreateFeatureDialog(project)
        if (dialog.showAndGet()) {
            val featureName = dialog.getFeatureName()
            val tribe = dialog.getTribe()

            // Pre-validate before attempting generation
            val featureDir = projectRoot.findChild("Feature")
            if (featureDir == null || !featureDir.isDirectory) {
                Messages.showErrorDialog(
                    "Project structure error: 'Feature' folder not found in project root.\n" +
                            "Please ensure your project has the correct folder structure.",
                    "Project Structure Error"
                )
                return
            }

            val tribeDir = featureDir.findChild(tribe)
            if (tribeDir == null || !tribeDir.isDirectory) {
                val availableTribes = featureDir.children
                    .filter { it.isDirectory }
                    .map { it.name }
                    .sorted()

                val message = if (availableTribes.isEmpty()) {
                    "Tribe folder 'Feature/$tribe' does not exist.\n" +
                            "No existing tribes found. Please create the tribe folder first."
                } else {
                    "Tribe folder 'Feature/$tribe' does not exist.\n\n" +
                            "Available tribes:\n${availableTribes.joinToString("\n") { "• $it" }}"
                }

                Messages.showErrorDialog(message, "Tribe Not Found")
                return
            }

            try {
                val generator = FeatureGenerator(projectRoot)
                generator.generateFeature(featureName, tribe)
                Messages.showInfoMessage(
                    "Feature modules created successfully!\n\n" +
                            "Created:\n" +
                            "• Feature/$tribe/feature-${
                                featureName.lowercase().replace('_', '-').replace(' ', '-')
                            }-api\n" +
                            "• Feature/$tribe/feature-${
                                featureName.lowercase().replace('_', '-').replace(' ', '-')
                            }-impl",
                    "Success"
                )
            } catch (ex: Exception) {
                Messages.showErrorDialog("Error creating feature: ${ex.message}", "Error")
            }
        }
    }

    private class CreateFeatureDialog(project: Project) : DialogWrapper(project) {
        private val featureNameField = JTextField(20)
        private val tribeField = JTextField(20)

        init {
            title = "Create Feature Module"
            init()
        }

        override fun createCenterPanel(): JComponent {
            val panel = JPanel(GridBagLayout())
            val gbc = GridBagConstraints().apply {
                insets = Insets(5, 5, 5, 5)
                anchor = GridBagConstraints.WEST
            }

            // Feature Name
            gbc.gridx = 0
            gbc.gridy = 0
            panel.add(JLabel("Feature Name:"), gbc)

            gbc.gridx = 1
            gbc.fill = GridBagConstraints.HORIZONTAL
            gbc.weightx = 1.0
            panel.add(featureNameField, gbc)

            // Tribe
            gbc.gridx = 0
            gbc.gridy = 1
            gbc.fill = GridBagConstraints.NONE
            gbc.weightx = 0.0
            panel.add(JLabel("Tribe:"), gbc)

            gbc.gridx = 1
            gbc.fill = GridBagConstraints.HORIZONTAL
            gbc.weightx = 1.0
            panel.add(tribeField, gbc)

            return panel
        }

        override fun doOKAction() {
            if (getFeatureName().trim().isEmpty() || getTribe().trim().isEmpty()) {
                Messages.showErrorDialog("Please fill in all fields", "Validation Error")
                return
            }
            super.doOKAction()
        }

        fun getFeatureName(): String = featureNameField.text.trim()
        fun getTribe(): String = tribeField.text.trim()
    }
}