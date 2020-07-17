package com.xora.thinkingwithportals_async

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Point
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.assets.RenderableSource
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    // Fragment variables
    private lateinit var arFragment: ArFragment

    // ===== State tracking ========================================================================

    // Variables for starting object placement
    private var isTracking: Boolean = false
    private var isHitting: Boolean = false
    private lateinit var axisAnchor: Anchor

    // Variables for communication states
    private var isAxisPlaced: Boolean = false
    private var isReceiving: Boolean = false

    // Variables for drawing state
    private var isDrawing: Boolean = false
    private var lines = ArrayList<Line>()

    // ===== Event listening =======================================================================

    // Startup
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize our ARFragment, add our frame listener
        arFragment = sceneform_fragment as ArFragment
        arFragment.arSceneView.scene.addOnUpdateListener { frameTime ->
            arFragment.onUpdate(frameTime)
            onUpdate()
        }

        // Button listeners
        placeButton.setOnClickListener {
            if (!isAxisPlaced) placeAxes() // Prevents double placement
        }

        drawButton.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (!isDrawing) {
                            isDrawing = true
                            lines.add(Line(arFragment))
                            lines.last().addVertexClient()
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        isDrawing = false
                    }
                }
                return true // Consume the event
            }
        })

        deleteButton.setOnClickListener {
            lines.forEach {
                it.removeLine()
            }

            lines.clear() // Clear the list
        }

        undoButton.setOnClickListener {
            lines.last().removeLine()
            lines.remove(lines.last())
        }

        commSwapButton.setOnClickListener {
            isReceiving = !isReceiving // Toggle receiving state
            updateCommColors()
            updateDrawingButtons()
        }

        // Hiding our FABs for initial state
        showFab(false, placeButton)
        showFab(false, drawButton)
        showFab(false, deleteButton)
        showFab(false, undoButton)
        showFab(false, commSwapButton)
        showFab(false, communicateButton)

        // Updating communication button state colors
        updateCommColors()
    }

    // On frame update
    private fun onUpdate() {
        // In the PLACE AXES state...
        if (!isAxisPlaced) {
            updateTracking()
            // If the device can find a plane...
            if (isTracking) {
                val hitTestChanged = updateHitTest()
                if (hitTestChanged) {
                    showFab(isHitting, placeButton) // Enable the button.
                }
            }
        }

        // In the DRAWING state...
        if (isDrawing) {
            lines.last().addVertexClient()
        }
    }

    // ===== State-agnostic functions ==============================================================

    // Show or hide a passed in FAB
    private fun showFab(enabled: Boolean, fab: FloatingActionButton) {
        fab.isEnabled = enabled // Set enabled boolean
        if (enabled) fab.visibility = View.VISIBLE
        else fab.visibility = View.GONE
    }

    // Update colors of communications buttons
    private fun updateCommColors() {
        if (isReceiving) { // Receiving state - Blue
            commSwapButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#0078FF"))
            communicateButton.backgroundTintList =
                ColorStateList.valueOf(Color.parseColor("#0078FF"))
            deleteButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#0078FF"))
        } else { // Transmitting state - Orange
            commSwapButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FD6600"))
            communicateButton.backgroundTintList =
                ColorStateList.valueOf(Color.parseColor("#FD6600"))
            deleteButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FD6600"))
        }
    }

    // Add a node to the scene
    private fun addAxisNodeToScene(anchor: Anchor, model: ModelRenderable) {
        // Create a (one time use) anchor node
        val anchorNode = AnchorNode(anchor)

        TransformableNode(arFragment.transformationSystem).apply {
            renderable = model
            setParent(anchorNode)
        }

        arFragment.arSceneView.scene.addChild(anchorNode) // Add the node to the scene
        axisAnchor = anchor // Save the anchor for later use
    }

    // ===== Place objects state ===================================================================

    // Check camera's tracking state, returns true if tracking changes
    private fun updateTracking(): Boolean {
        val frame = arFragment.arSceneView.arFrame
        val wasTracking = isTracking
        frame ?: return false // Nullable handling, probably breaks things
        isTracking = frame.camera.trackingState == TrackingState.TRACKING
        return isTracking != wasTracking
    }

    // Determines whether camera center is intersecting with some geometry
    private fun updateHitTest(): Boolean {
        val frame = arFragment.arSceneView.arFrame
        val point = getScreenCenter()
        val hits: List<HitResult>
        val wasHitting = isHitting
        isHitting = false
        if (frame != null) {
            hits = frame.hitTest(point.x.toFloat(), point.y.toFloat())
            for (hit in hits) {
                val trackable = hit.trackable
                if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                    isHitting = true
                    break
                }
            }
        }
        return wasHitting != isHitting
    }

    // Returns the center of the screen
    private fun getScreenCenter(): Point {
        val view = findViewById<View>(android.R.id.content)
        return Point(view.width / 2, view.height / 2)
    }

    // Place the axes in the world
    private fun placeAxes() {
        val frame = arFragment.arSceneView.arFrame
        val point = getScreenCenter()
        if (frame != null) {
            val hits = frame.hitTest(point.x.toFloat(), point.y.toFloat())
            for (hit in hits) {
                val trackable = hit.trackable
                if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                    createAxesModelRenderableThenPlaceIt(hit.createAnchor())
                    break
                }
            }
        }
    }

    // Private-r function to create the axes model
    private fun createAxesModelRenderableThenPlaceIt(anchor: Anchor) {
        // Poly.google.com axes model
        val model =
            Uri.parse("https://poly.googleusercontent.com/downloads/c/fp/1594805866743956/3ryLFYBNHCF/czTDFVl1MhQ/axis.gltf")

        // Do the thing!
        ModelRenderable.builder()
            .setSource(
                arFragment.context,
                RenderableSource.builder()
                    .setSource(
                        arFragment.context,
                        model,
                        RenderableSource.SourceType.GLTF2
                    )
                    .setScale(0.25f)
                    .build()
            )
            .setRegistryId(model)
            .build()
            .thenAccept {
                addAxisNodeToScene(anchor, it)

                // Update the button state!
                changePlaceToGameState()
            }
            .exceptionally {
                Toast.makeText(
                    this@MainActivity,
                    "Could not fetch model from $model",
                    Toast.LENGTH_SHORT
                )
                    .show()
                return@exceptionally null
            }
    }

    // ===== Game state ============================================================================

    // Changes the buttons and their placement to the proper game state
    private fun changePlaceToGameState() {
        // Change state variable
        isAxisPlaced = true

        // Change button visibility
        showFab(false, placeButton)
        showFab(true, commSwapButton)
        showFab(true, communicateButton)
        showFab(true, deleteButton)
        updateDrawingButtons()

        // Update communication button colors
        updateCommColors()
    }

    // Changes visibility of buttons based on receiving state
    private fun updateDrawingButtons() {
        if (isReceiving) { // Receiving state
            showFab(false, drawButton)
            showFab(false, undoButton)
        } else { // Transmitting state
            showFab(true, drawButton)
            showFab(true, undoButton)
        }
    }

}