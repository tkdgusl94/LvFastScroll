package leveloper.lvfastscroll

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import leveloper.lvfastscroll.databinding.LayoutScrollerBinding
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class LvFastScroll @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val binding: LayoutScrollerBinding

    var recyclerView: RecyclerView? = null
        set(value) {
            field = value
            field?.run {
                addOnScrollListener(scrollListener)
            }
        }

    private val scrollListener = ScrollListener()
    private var viewHeight: Int = 0

    private val hideScrollerSubject = PublishSubject.create<Boolean>()
    private var hideDisposable: Disposable? = null

    @ColorInt private var trackColor: Int = 0
    @ColorInt private var handleColor: Int = 0
    @ColorInt private var bubbleColor: Int = 0
    @ColorInt private var bubbleTextColor: Int = 0

    init {
        orientation = HORIZONTAL
        binding = LayoutScrollerBinding.inflate(LayoutInflater.from(context), this, true)

        context.obtainStyledAttributes(attrs, R.styleable.LvFastScroll, defStyleAttr, defStyleRes).apply {
            trackColor = getColor(R.styleable.LvFastScroll_lv_trackColor, Color.LTGRAY)
            handleColor = getColor(R.styleable.LvFastScroll_lv_handleColor, Color.DKGRAY)
            bubbleColor = getColor(R.styleable.LvFastScroll_lv_bubbleColor, Color.BLUE)
            bubbleTextColor = getColor(R.styleable.LvFastScroll_lv_bubbleTextColor, Color.WHITE)

            initColor()

            recycle()
        }

        setupHideScrollerSubscribe()
    }

    private fun initColor() {
        binding.ivTrack.imageTintList = ColorStateList.valueOf(trackColor)
        binding.ivHandle.imageTintList = ColorStateList.valueOf(handleColor)

        DrawableCompat.setTint(binding.tvBubble.background, bubbleColor)
        binding.tvBubble.setTextColor(bubbleTextColor)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isTouchScroller(event)) {
                    binding.clHandle.isSelected = true
                    showScroller(event)
                    showBubble()
                    setHandleColor(bubbleColor)

                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (binding.clHandle.isSelected) {
                    showScroller(event)
                    hideScrollerSubject.onNext(true)

                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                binding.clHandle.isSelected = false
                hideBubble()
                setHandleColor(handleColor)

                return false
            }
            else -> return super.onTouchEvent(event)
        }
        return false
    }

    private fun setHandleColor(@ColorInt color: Int) {
        binding.ivHandle.imageTintList = ColorStateList.valueOf(color)
    }

    private fun isTouchScroller(event: MotionEvent): Boolean {
        val scrollerRect = Rect().apply {
            binding.clTrack.getHitRect(this)
        }
        return scrollerRect.contains(event.x.toInt(), event.y.toInt())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewHeight = h
    }

    private fun setupHideScrollerSubscribe() {
        /* 1초 후에 반환 */
        hideDisposable = hideScrollerSubject.debounce(HIDE_DELAY_SECOND, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .filter { !binding.clHandle.isSelected }
            .subscribe({
                hideAnimateHandle()
            }, { throwable -> throwable.printStackTrace() })
    }

    private fun showScroller(event: MotionEvent) {
        setScrollerPosition(event.y)
        setRecyclerViewPosition(event.y)
    }

    private fun setScrollerPosition(positionY: Float) {
        binding.clHandle.y = getValueInRange(
            positionY - (binding.clHandle.height / 2),
            viewHeight - binding.clHandle.height
        )

        binding.clBubble.y = getValueInRange(
            positionY - (binding.clHandle.height),
            viewHeight - binding.clBubble.height
        )
    }

    private fun setRecyclerViewPosition(positionY: Float) {
        recyclerView?.adapter?.run {
            val proportion: Float = when {
                binding.clHandle.y == 0f -> 0f
                binding.clHandle.y + binding.clHandle.height >= viewHeight - SCROLLER_MAX_POSITION_GAP -> 1f
                else -> positionY / viewHeight
            }
            val targetPos: Float = getValueInRange(proportion * itemCount, itemCount - 1)

            (recyclerView?.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(
                targetPos.roundToInt(),
                0
            )
        }
    }

    private fun getValueInRange(value: Float, max: Int): Float = value.coerceIn(0f, max.toFloat())

    private fun showAnimateHandle() {
        binding.clTrack.visibility = View.VISIBLE

        ObjectAnimator.ofFloat(
            binding.clTrack,
            TRANSLATION_X,
            binding.clHandle.width.toFloat(),
            0f
        ).apply {
            duration = ANIMATION_TIME_HANDLE
        }.start()
    }


    private fun hideAnimateHandle() {
        if (binding.clTrack.visibility == View.GONE) {
            return
        }

        ObjectAnimator.ofFloat(
            binding.clTrack,
            TRANSLATION_X,
            0f,
            binding.clHandle.width.toFloat()
        ).apply {
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    binding.clTrack.visibility = View.GONE
                }

                override fun onAnimationCancel(animation: Animator) {
                    super.onAnimationCancel(animation)
                    binding.clTrack.visibility = View.GONE
                }
            })

            duration = ANIMATION_TIME_HANDLE
        }.start()
    }

    private fun showBubble() {
        if (binding.clBubble.visibility == View.VISIBLE) {
            return
        }
        binding.clBubble.visibility = View.VISIBLE
        ObjectAnimator.ofFloat(
            binding.clBubble,
            TRANSLATION_X,
            binding.clBubble.width.toFloat(),
            0f
        ).apply {
            duration = ANIMATION_TIME_BUBBLE
        }.start()
    }

    private fun hideBubble() {
        if (binding.clBubble.visibility == View.GONE) {
            return
        }
        ObjectAnimator.ofFloat(
            binding.clBubble,
            TRANSLATION_X,
            0f,
            binding.clBubble.width.toFloat()
        ).apply {
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    binding.clBubble.visibility = View.GONE

                }

                override fun onAnimationCancel(animation: Animator) {
                    super.onAnimationCancel(animation)
                    binding.clBubble.visibility = View.GONE
                }
            })
            duration = ANIMATION_TIME_BUBBLE
        }.start()
    }

    override fun onDetachedFromWindow() {
        recyclerView?.removeOnScrollListener(scrollListener)
        hideDisposable?.dispose()
        super.onDetachedFromWindow()
    }

    private inner class ScrollListener : RecyclerView.OnScrollListener() {
        override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
            if (dy == 0) return

            (rv.layoutManager as? LinearLayoutManager)?.run {
                if (binding.clTrack.visibility == View.GONE) {
                    showAnimateHandle()
                }

                updateBubbleAndHandlePosition()
                hideScrollerSubject.onNext(true)
            }
        }
    }

    private fun updateBubbleAndHandlePosition() {
        if (binding.clHandle.isSelected) {
            return
        }
        recyclerView?.let {
            val verticalScrollOffset = it.computeVerticalScrollOffset()
            val verticalScrollRange = it.computeVerticalScrollRange()
            val proportion =
                verticalScrollOffset.toFloat() / (verticalScrollRange.toFloat() - viewHeight)
            setScrollerPosition(viewHeight * proportion)
        }
    }

    fun setBubbleText(text: String) {
        binding.tvBubble.text = text
    }

    companion object {
        private const val HIDE_DELAY_SECOND: Long = 1
        private const val SCROLLER_MAX_POSITION_GAP: Long = 5
        private const val TRANSLATION_X = "translationX"
        private const val ANIMATION_TIME_HANDLE = 400L
        private const val ANIMATION_TIME_BUBBLE = 150L
    }
}