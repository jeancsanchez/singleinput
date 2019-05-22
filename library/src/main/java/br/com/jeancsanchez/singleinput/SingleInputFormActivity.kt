/*
 * MIT License
 *
 * Copyright (c) 2017 Jan Heinrich Reimer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package br.com.jeancsanchez.singleinput

import android.animation.ObjectAnimator
import android.content.res.Resources
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Property
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import br.com.jeancsanchez.singleinput.steps.Step
import kotlinx.android.synthetic.main.activity_single_input_form.*
import java.util.*

abstract class SingleInputFormActivity : AppCompatActivity() {

    private val KEY_DATA = "key_data"
    private val KEY_STEP_INDEX = "key_step_index"

    private val PB_PROGRESS_PROPERTY = object : Property<ProgressBar, Int>(Int::class.java, "PB_PROGRESS_PROPERTY") {

        override fun set(pb: ProgressBar, value: Int?) {
            pb.progress = value ?: 0
        }

        override fun get(pb: ProgressBar): Int {
            return pb.progress
        }
    }

    private var steps: List<Step> = ArrayList()
    private var setupData: Bundle? = Bundle()
    private var stepIndex = 0
    protected var error: Boolean = false

    private val nextButtonClickListener = View.OnClickListener { nextStep() }

    private var buttonNextIcon: Drawable? = null
    private var buttonFinishIcon: Drawable? = null

    private var textFieldBackgroundColor = -1
    private var progressBackgroundColor = -1

    private var titleTextColor = -1
    private var detailsTextColor = -1
    private var errorTextColor = -1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_input_form)

        loadTheme()
        steps = onCreateSteps()

        if (savedInstanceState != null) {
            setupData = savedInstanceState.getBundle(KEY_DATA)
            stepIndex = savedInstanceState.getInt(KEY_STEP_INDEX, 0)
        }

        setupTitle()
        setupInput()
        setupError()
        setupDetails()

        nextButton?.setOnClickListener(nextButtonClickListener)
        previousButton?.setOnClickListener { _ -> onBackPressed() }
        errorSwitcher?.setText("")
        updateStep()
    }

    override fun onBackPressed() {
        if (stepIndex == 0) {
            finish()
        } else {
            previousStep()
        }
    }


    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)

        if (savedInstanceState != null) {
            setupData = savedInstanceState.getBundle(KEY_DATA)
            stepIndex = savedInstanceState.getInt(KEY_STEP_INDEX, 0)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        setupData = getCurrentStep().save(setupData)
        outState.putBundle(KEY_DATA, setupData)
        outState.putInt(KEY_STEP_INDEX, stepIndex)
    }

    override fun onPause() {
        hideSoftInput()
        super.onPause()
    }

    protected abstract fun onCreateSteps(): List<Step>


    protected fun getCurrentStep(): Step {
        return getStep(stepIndex)
    }

    protected fun getStep(position: Int): Step {
        return steps[position]
    }

    private fun loadTheme() {
        /* Default values */
        buttonNextIcon = ContextCompat.getDrawable(this, R.drawable.ic_arrow_forward)
        buttonFinishIcon = ContextCompat.getDrawable(this, R.drawable.ic_done)


        /* Custom values */
        val attrs = intArrayOf(R.attr.colorPrimary, R.attr.colorPrimaryDark, android.R.attr.textColorPrimary, android.R.attr.textColorSecondary, R.attr.sifNextIcon, R.attr.sifFinishIcon)
        val array = obtainStyledAttributes(attrs)!!

        textFieldBackgroundColor = array.getColor(0, 0)
        progressBackgroundColor = array.getColor(1, 0)
        errorTextColor = array.getColor(2, 0)
        titleTextColor = errorTextColor
        detailsTextColor = array.getColor(3, 0)

        val buttonNextIcon = array.getDrawable(4)
        if (buttonNextIcon != null) {
            this.buttonNextIcon = buttonNextIcon
        }

        val buttonFinishIcon = array.getDrawable(0)
        if (buttonFinishIcon != null) {
            this.buttonFinishIcon = buttonFinishIcon
        }

        array.recycle()
    }

    private fun getAnimation(animationResId: Int, isInAnimation: Boolean): Animation {
        val interpolator: Interpolator

        if (isInAnimation) {
            interpolator = DecelerateInterpolator(1.0f)
        } else {
            interpolator = AccelerateInterpolator(1.0f)
        }

        val animation = AnimationUtils.loadAnimation(this, animationResId)
        animation.interpolator = interpolator

        return animation
    }

    private fun setupTitle() {
        titleSwitcher?.inAnimation = getAnimation(R.anim.slide_in_to_bottom, true)
        titleSwitcher?.outAnimation = getAnimation(R.anim.slide_out_to_top, false)

        titleSwitcher?.setFactory {
            val view = layoutInflater?.inflate(R.layout.view_title, titleSwitcher, false) as TextView
            view.setTextColor(titleTextColor)
            view
        }

        titleSwitcher?.setText("")
    }

    private fun setupInput() {
        inputSwitcher?.inAnimation = getAnimation(R.anim.alpha_in, true)
        inputSwitcher?.outAnimation = getAnimation(R.anim.alpha_out, false)

        inputSwitcher?.removeAllViews()
        for (i in steps.indices) {
            inputSwitcher?.addView(getStep(i).view)
        }
    }

    private fun setupError() {
        errorSwitcher?.inAnimation = getAnimation(android.R.anim.slide_in_left, true)
        errorSwitcher?.outAnimation = getAnimation(android.R.anim.slide_out_right, false)

        errorSwitcher?.setFactory {
            val view = layoutInflater?.inflate(R.layout.view_error, titleSwitcher, false) as TextView?
            view?.let {
                if (errorTextColor != -1) {
                    view.setTextColor(errorTextColor)
                }
                it
            }
        }

        errorSwitcher?.setText("")
    }

    private fun setupDetails() {
        detailsSwitcher?.inAnimation = getAnimation(R.anim.alpha_in, true)
        detailsSwitcher?.outAnimation = getAnimation(R.anim.alpha_out, false)

        detailsSwitcher?.setFactory {
            val view = layoutInflater?.inflate(R.layout.view_details, titleSwitcher, false) as TextView?
            view?.let {
                if (detailsTextColor != -1) {
                    view.setTextColor(detailsTextColor)
                }
                view
            }
        }

        detailsSwitcher?.setText("")
    }

    private fun updateStep() {
        if (stepIndex > 0) {
            previousButton?.visibility = View.VISIBLE
        } else {
            previousButton?.visibility = View.GONE
        }

        if (stepIndex >= steps.size) {
            hideSoftInput()

            val finishedView = onCreateFinishedView(layoutInflater!!, container)
            if (finishedView != null) {
                finishedView.alpha = 0f
                finishedView.visibility = View.VISIBLE
                container?.addView(finishedView)
                finishedView.animate()
                        .alpha(1f).duration = resources.getInteger(
                        android.R.integer.config_mediumAnimTime).toLong()
            }

            onFormFinished(setupData)
            return
        }
        updateViews()
        containerScrollView?.smoothScrollTo(0, 0)
    }

    protected fun hideSoftInput() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
        val v = currentFocus ?: return
        imm?.hideSoftInputFromWindow(v.windowToken, 0)
    }

    protected fun onCreateFinishedView(inflater: LayoutInflater, parent: ViewGroup?): View? {
        return null
    }

    protected abstract fun onFormFinished(data: Bundle?)

    private fun updateViews() {
        val step = getCurrentStep()

        if (stepIndex + 1 >= steps.size) {
            nextButton?.contentDescription = getString(R.string.finish)
            step.updateView(true)
        } else {
            nextButton?.setImageDrawable(buttonNextIcon)
            nextButton?.contentDescription = getString(R.string.next_step)
            step.updateView(false)
        }

        step.restore(setupData)

        setTextFieldBackgroundDrawable()

        inputSwitcher?.displayedChild = stepIndex
        errorSwitcher?.setText("")
        detailsSwitcher?.setText(step.getDetails(this))
        titleSwitcher?.setText(step.getTitle(this))
        stepText?.text = getString(R.string.page_number, stepIndex + 1, steps.size)

        stepText?.setTextColor(detailsTextColor)

        updateProgressbar()
    }

    private fun setTextFieldBackgroundDrawable() {
        if (textFieldBackgroundColor != -1) {
            textField?.setBackgroundColor(textFieldBackgroundColor)
        }
    }

    private fun setProgressDrawable() {
        if (progressBackgroundColor != -1) {
            val progressDrawable = progress?.progressDrawable
            progressDrawable?.setColorFilter(progressBackgroundColor, PorterDuff.Mode.SRC_IN)
        }
    }

    private fun updateProgressbar() {
        progress?.max = steps.size * 100
        ObjectAnimator.ofInt(progress, PB_PROGRESS_PROPERTY, stepIndex * 100).start()
    }

    protected fun previousStep() {
        setupData = getCurrentStep().save(setupData)
        stepIndex--
        updateStep()
    }

    protected fun nextStep() {
        val step = getCurrentStep()
        val checkStep = checkStep()
        if (!checkStep) {
            if (!error) {
                error = true
                try {
                    errorSwitcher?.setText(step.getError(this))
                } catch (exception: Resources.NotFoundException) {
                    exception.printStackTrace()
                    errorSwitcher?.setText("")
                }

            }
        } else {
            error = false
        }
        if (error) {
            return
        }
        setupData = step.save(setupData)

        stepIndex++
        updateStep()
    }

    protected fun forceNextStep() {
        error = false
        val step = getCurrentStep()
        setupData = step.save(setupData)
        stepIndex++
        updateStep()
    }

    private fun checkStep(): Boolean {
        return getCurrentStep().validate()
    }

    fun setInputGravity(gravity: Int) {
        val layoutParams = innerContainer?.layoutParams as FrameLayout.LayoutParams
        layoutParams.gravity = gravity
        innerContainer?.layoutParams = layoutParams
    }


    fun getInputGravity(): Int {
        val layoutParams = innerContainer?.layoutParams as FrameLayout.LayoutParams
        return layoutParams.gravity
    }
}