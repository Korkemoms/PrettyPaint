/*
 *
 *  The MIT License (MIT)
 *
 *  Copyright (c) <2015> <Andreas Modahl>
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 */
package org.ams.core;

import com.badlogic.gdx.utils.Array;

/**
 * Simple timer that works with render count instead of time. Not thread safe so use only from one thread.
 * See {@link #step()}.
 *
 * @author Andreas
 */
public class Timer {



        private Array<TimedTask> oneTimeTasks = new Array<TimedTask>(false, 10, TimedTask.class);
        private Array<TimedTask> eachRenderTasks = new Array<TimedTask>(false, 10, TimedTask.class);


        /** Run after n render. See {@link #step()}. */
        public void runAfterNRender(Runnable task, long n) {
                TimedTask tt = new TimedTask();
                tt.task = task;
                tt.n = n;
                oneTimeTasks.add(tt);
        }

        /** Run after each render. See {@link #step()}. */
        public void runOnRender(Runnable task) {
                TimedTask tt = new TimedTask();
                tt.task = task;
                eachRenderTasks.add(tt);
        }

        public boolean contains(Runnable task) {
                for (TimedTask timedTask : oneTimeTasks) {
                        if (timedTask.task == task) return true;
                }
                for (TimedTask timedTask : eachRenderTasks) {
                        if (timedTask.task == task) return true;
                }

                return false;
        }


        /** Should be called at the end of the render() method in your {@link com.badlogic.gdx.ApplicationListener}. */
        public void step() {
                for (int i = oneTimeTasks.size - 1; i >= 0; i--) {
                        TimedTask timedTask = oneTimeTasks.items[i];
                        timedTask.n--;

                        if (timedTask.n <= 0) {
                                oneTimeTasks.items[i].task.run();
                                oneTimeTasks.removeIndex(i);
                        }
                }
                for (int i = eachRenderTasks.size - 1; i >= 0; i--) {
                        eachRenderTasks.items[i].task.run();
                }
        }

        /** Stop the task from executing in the future. */
        public void remove(Runnable task) {
                for (int i = oneTimeTasks.size - 1; i >= 0; i--) {
                        if (oneTimeTasks.items[i].task == task) {
                                oneTimeTasks.removeIndex(i);
                        }
                }
                for (int i = eachRenderTasks.size - 1; i >= 0; i--) {
                        if (eachRenderTasks.items[i].task == task) {
                                eachRenderTasks.removeIndex(i);
                        }
                }
        }

        public void clear() {
                oneTimeTasks.clear();
                eachRenderTasks.clear();
        }

        public static class TimedTask {

                public long n;

                public Runnable task;
        }


}
