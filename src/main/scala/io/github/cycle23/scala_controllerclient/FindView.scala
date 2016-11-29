package io.github.cycle23.scala_controllerclient

import android.app.Activity
import android.view.View

/**
  * Created by cody on 11/29/16.
  */
trait FindView extends Activity {
  // TODO: Use Option or Either
  def findView [WidgetType] (id: Int) : WidgetType = {
    findViewById(id) match {
      case wt: WidgetType => wt
      case ot => throw new ClassCastException
      case null => throw new NullPointerException
    }
  }
}

object FindView extends Activity
