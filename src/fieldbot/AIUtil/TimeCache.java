/*
 * Copyright (C) 2018 PlayerO1
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package fieldbot.AIUtil;

/**
 * Time-base cache.
 * Thread-safe, but not testing in MT - bee ware use of it.
 * @author PlayerO1
 */
public abstract class TimeCache<T> {
    protected T cacheValue;
    private volatile long time=0;
    private final long invalidateTimeMs;

    public TimeCache(long invalidateTimeMs) {
        this.invalidateTimeMs = invalidateTimeMs;
        if (invalidateTimeMs<=0) throw new IllegalArgumentException("Use positive time in ms.");
    }
    
    public T get() {
        if (System.currentTimeMillis()-time>invalidateTimeMs) {
            // System.currentTimeMillis() never equals 0.
            invalidateNow();
        }
        return cacheValue;
    }
    
    public synchronized void invalidateNow() {
        cacheValue=newValue();
        time=System.currentTimeMillis();
    }
    
    protected abstract T newValue();
}
