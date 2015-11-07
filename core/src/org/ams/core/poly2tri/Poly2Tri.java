/* Poly2Tri
 * Copyright (c) 2009-2010, Poly2Tri Contributors
 * http://code.google.com/p/poly2tri/
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * * Neither the name of Poly2Tri nor the names of its contributors may be
 *   used to endorse or promote products derived from this software without specific
 *   prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.ams.core.poly2tri;

import org.ams.core.poly2tri.geometry.polygon.Polygon;
import org.ams.core.poly2tri.geometry.polygon.PolygonSet;
import org.ams.core.poly2tri.triangulation.Triangulatable;
import org.ams.core.poly2tri.triangulation.TriangulationAlgorithm;
import org.ams.core.poly2tri.triangulation.TriangulationContext;
import org.ams.core.poly2tri.triangulation.delaunay.sweep.DTSweep;
import org.ams.core.poly2tri.triangulation.delaunay.sweep.DTSweepContext;
import org.ams.core.poly2tri.triangulation.sets.ConstrainedPointSet;
import org.ams.core.poly2tri.triangulation.sets.PointSet;


public class Poly2Tri
{

    private static final TriangulationAlgorithm _defaultAlgorithm = TriangulationAlgorithm.DTSweep;
    
    public static void triangulate( PolygonSet ps )
    {
        TriangulationContext<?> tcx = createContext( _defaultAlgorithm );
        for( Polygon p : ps.getPolygons() )
        {
            tcx.prepareTriangulation( p );
            triangulate( tcx );            
            tcx.clear();
        }
    }

    public static void triangulate( Polygon p )
    {
        triangulate( _defaultAlgorithm, p );            
    }

    public static void triangulate( ConstrainedPointSet cps )
    {
        triangulate( _defaultAlgorithm, cps );        
    }

    public static void triangulate( PointSet ps )
    {
        triangulate( _defaultAlgorithm, ps );                
    }

    public static TriangulationContext<?> createContext( TriangulationAlgorithm algorithm )
    {
        switch( algorithm )
        {
            case DTSweep:
            default:
                return new DTSweepContext();
        }
    }

    public static void triangulate( TriangulationAlgorithm algorithm,
                                    Triangulatable t )
    {
        TriangulationContext<?> tcx;
        
//        long time = System.nanoTime();
        tcx = createContext( algorithm );
        tcx.prepareTriangulation( t );
        triangulate( tcx );
//        logger.info( "Triangulation of {} points [{}ms]", tcx.getPoints().size(), ( System.nanoTime() - time ) / 1e6 );
    }
    
    public static void triangulate( TriangulationContext<?> tcx )
    {
        switch( tcx.algorithm() )
        {
            case DTSweep:
            default:
               DTSweep.triangulate( (DTSweepContext)tcx );
        }        
    }
    

}
