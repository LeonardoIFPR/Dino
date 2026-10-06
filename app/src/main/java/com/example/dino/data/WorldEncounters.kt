package com.example.dino.data

import java.util.Random
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor

const val DISCOVERY_RADIUS_METERS = 400f
const val COLLECTION_RADIUS_METERS = 100f
private const val LATITUDE_STEP = .0018
private const val LATITUDE_ROWS = 100_000

/** Stable virtual finds across the globe; their positions do not depend on the player's origin. */
fun worldEncounters(player:GeoPoint):List<Encounter> {
    val centreRow=floor((player.latitude+90)/LATITUDE_STEP).toInt().coerceIn(0,LATITUDE_ROWS-1)
    val longitude=((player.longitude+180)%360+360)%360
    val finds=mutableListOf<Encounter>()
    for(row in (centreRow-3).coerceAtLeast(0)..(centreRow+3).coerceAtMost(LATITUDE_ROWS-1)) {
        val rowLatitude=-90+(row+.5)*LATITUDE_STEP
        val columns=ceil(360*111_320*cos(Math.toRadians(rowLatitude))/200).toInt().coerceAtLeast(1)
        val longitudeStep=360.0/columns
        val centreColumn=floor(longitude/longitudeStep).toInt().coerceAtMost(columns-1)
        for(column in (-3..3).map {((centreColumn+it)%columns+columns)%columns}.distinct()) {
            val random=Random((row.toLong()*73_856_093L) xor (column.toLong()*19_349_663L) xor 0x464F5353494CL)
            if(random.nextInt(100)>=38) continue
            val point=GeoPoint(-90+(row+.25+random.nextDouble()*.5)*LATITUDE_STEP,
                -180+(column+.25+random.nextDouble()*.5)*longitudeStep)
            if(distance(player,point)>DISCOVERY_RADIUS_METERS) continue
            val fossil=Catalog.fossils[random.nextInt(Catalog.fossils.size)]
            val part=fossil.fieldParts[random.nextInt(fossil.fieldParts.size)]
            finds.add(Encounter("world-v1:$row:$column",fossil.id,part.id,point))
        }
    }
    return finds.sortedBy {distance(player,it.point)}
}
