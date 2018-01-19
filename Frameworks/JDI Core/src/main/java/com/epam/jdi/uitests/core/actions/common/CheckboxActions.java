package com.epam.jdi.uitests.core.actions.common;

/* The MIT License
 *
 * Copyright 2004-2017 EPAM Systems
 *
 * This file is part of JDI project.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:

 * The above copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 */

/**
 * Created by Roman Iovlev on 10.03.2017
 */
import com.epam.jdi.tools.func.JAction1;
import com.epam.jdi.tools.func.JFunc1;

import static com.epam.jdi.uitests.core.actions.common.ClickActions.click;
import static com.epam.jdi.uitests.core.settings.JDISettings.exception;

public abstract class CheckboxActions {
    // () -> element.isChecked() -> Boolean
    public static JFunc1<Object, Boolean> isChecked;
    // () -> element.check()
    public static JAction1<Object> check = o -> {
        if (!isChecked.execute(o))
            click.execute(o);
        if (!isChecked.execute(o))
            throw exception("Can't check element. Verify locator for click or isCheckedAction");
    };
    // () -> element.uncheck()
    public static JAction1<Object> uncheck = o -> {
        if (isChecked.execute(o))
            click.execute(o);
        if (isChecked.execute(o))
            throw exception("Can't uncheck element. Verify locator for click or isCheckedAction");
    };
}