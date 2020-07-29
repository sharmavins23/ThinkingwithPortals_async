package com.xora.thinkingwithportals_async

import android.graphics.Color.WHITE
import com.google.ar.core.Anchor
import com.google.ar.core.Pose
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode

class Line(private val arFragment: ArFragment, private val axisAnchor: Anchor) {
    // Fragment variables - Nulls are ignored at my own risk
    private val frame = arFragment.arSceneView.arFrame!!
    private val session = arFragment.arSceneView.session!!
    private val scene = arFragment.arSceneView.scene

    // Keeping track of our line points for deletion purposes
    private var vertices = ArrayList<AnchorNode>()

    // Keeping track of our size variables for later
    private val lineRadius: Float = 0.01f
    private val distanceAwayFromCamera = 0f // negative is away from user

    // ===== Creator-agnostic functions ============================================================

    private fun makeDotModel(anchor: Anchor) {
        val point = AnchorNode(anchor).worldPosition

        MaterialFactory.makeOpaqueWithColor(arFragment.context, Color(WHITE)).thenAccept {
            addNodeToScene(anchor, ShapeFactory.makeSphere(lineRadius, point, it).apply {
                this.isShadowCaster = false
                this.isShadowReceiver = false
            })
        }
    }

    // Adds a node to the scene, with the associated object, and stores the anchornode for later
    private fun addNodeToScene(anchor: Anchor, model: ModelRenderable) {
        // Create a (one time use) anchor node
        val anchorNode = AnchorNode(anchor)

        TransformableNode(arFragment.transformationSystem).apply {
            renderable = model
            setParent(anchorNode)
        }

        scene.addChild(anchorNode) // Add the node to the scene
        vertices.add(anchorNode) // Save the anchornode for later use
    }

    // Deletes all points in a line
    fun removeLine() {
        vertices.forEach {
            scene.removeChild(it) // Remove from the scene
            it.anchor?.detach()
            it.setParent(null)
            it.renderable = null // Stop rendering
        }
    }

    // Returns the vector3 difference of a point's anchornode's anchor and the axis anchor.
    private fun vectorDiff(pointAnchor: AnchorNode): Vector3 {
        // Get poses
        val axis = axisAnchor.pose
        val point = pointAnchor.anchor!!.pose

        // Calculate the vector difference
        return Vector3(
            point.tx() - axis.tx(),
            point.ty() - axis.ty(),
            point.tz() - axis.tz()
        )
    }

    // ===== Client vertex creation ================================================================

    // Creates a vertex that the client has drawn (wrapper function)
    fun addVertexClient() {
        // Get the anchor, make the model, put it in the world. Done.
        makeDotModel(getCameraAnchor())
    }

    // Creates an anchor in front of the camera.
    private fun getCameraAnchor(): Anchor {
        // Creates an anchor out of the camera position and returns it
        return session.createAnchor(
            frame.camera.displayOrientedPose.compose(
                Pose.makeTranslation(
                    0f, // x
                    0f, // y
                    distanceAwayFromCamera // z
                )
            ).extractTranslation()
        )
    }

    // ===== Server-sent vertex creation ===========================================================

    // Creates a vertex that someone has sent
    fun addVertexServer() {

    }
}