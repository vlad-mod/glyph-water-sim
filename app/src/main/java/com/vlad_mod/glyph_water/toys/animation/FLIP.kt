package com.vlad_mod.glyph_water.toys.animation

import kotlin.math.*

/**
 * This code is translated from javascript and is made by TenMinutePhysics
 * https://github.com/matthias-research/pages/blob/master/tenMinutePhysics/18-flip.html
 */
class FLIP(
    val density: Double,
    width: Double,
    height: Double,
    spacing: Double,
    val particleRadius: Double,
    val maxParticles: Int
) {
    val fNumX = floor(width / spacing).toInt() + 1
    val fNumY = floor(height / spacing).toInt() + 1
    val h = max(width / fNumX, height / fNumY)
    val fInvSpacing = 1.0 / h
    val fNumCells = fNumX * fNumY

    val u = DoubleArray(fNumCells)
    val v = DoubleArray(fNumCells)
    val du = DoubleArray(fNumCells)
    val dv = DoubleArray(fNumCells)
    val prevU = DoubleArray(fNumCells)
    val prevV = DoubleArray(fNumCells)
    val p = DoubleArray(fNumCells)
    val s = DoubleArray(fNumCells)
    val cellType = IntArray(fNumCells)
    val cellColor = DoubleArray(3 * fNumCells)

    val particlePos = DoubleArray(2 * maxParticles)
    val particleColor = DoubleArray(3 * maxParticles).apply {
        for (i in 0 until maxParticles) this[3 * i + 2] = 1.0
    }
    val particleVel = DoubleArray(2 * maxParticles)
    val particleDensity = DoubleArray(fNumCells)
    var particleRestDensity = 0.0

    val pInvSpacing = 1.0 / (2.2f * particleRadius)
    val pNumX = floor(width * pInvSpacing).toInt() + 1
    val pNumY = floor(height * pInvSpacing).toInt() + 1
    val pNumCells = pNumX * pNumY

    val numCellParticles = IntArray(pNumCells)
    val firstCellParticle = IntArray(pNumCells + 1)
    val cellParticleIds = IntArray(maxParticles)

    var numParticles = 0

    fun integrateParticles(
        dt: Double,
        gravityX: Double,
        gravityY: Double
    ) {
        for (i in 0 until numParticles) {
            particleVel[2 * i] += dt * gravityX
            particleVel[2 * i + 1] += dt * gravityY
            particlePos[2 * i] += particleVel[2 * i] * dt
            particlePos[2 * i + 1] += particleVel[2 * i + 1] * dt
        }
    }

    fun pushParticlesApart(numIters: Int) {
        val colorDiffusionCoeff = 0.001f
        numCellParticles.fill(0)

        for (i in 0 until numParticles) {
            val x = particlePos[2 * i]
            val y = particlePos[2 * i + 1]
            val xi = clamp(floor(x * pInvSpacing).toInt(), 0, pNumX - 1)
            val yi = clamp(floor(y * pInvSpacing).toInt(), 0, pNumY - 1)
            val cellNr = xi * pNumY + yi
            numCellParticles[cellNr]++
        }

        var first = 0
        for (i in 0 until pNumCells) {
            first += numCellParticles[i]
            firstCellParticle[i] = first
        }
        firstCellParticle[pNumCells] = first

        for (i in 0 until numParticles) {
            val x = particlePos[2 * i]
            val y = particlePos[2 * i + 1]
            val xi = clamp(floor(x * pInvSpacing).toInt(), 0, pNumX - 1)
            val yi = clamp(floor(y * pInvSpacing).toInt(), 0, pNumY - 1)
            val cellNr = xi * pNumY + yi
            firstCellParticle[cellNr]--
            cellParticleIds[firstCellParticle[cellNr]] = i
        }

        val minDist = 2.0f * particleRadius
        val minDist2 = minDist * minDist

        repeat(numIters) {
            for (i in 0 until numParticles) {
                val px = particlePos[2 * i]
                val py = particlePos[2 * i + 1]

                val pxi = floor(px * pInvSpacing).toInt()
                val pyi = floor(py * pInvSpacing).toInt()
                val x0 = max(pxi - 1, 0)
                val y0 = max(pyi - 1, 0)
                val x1 = min(pxi + 1, pNumX - 1)
                val y1 = min(pyi + 1, pNumY - 1)

                for (xi in x0..x1) {
                    for (yi in y0..y1) {
                        val cellNr = xi * pNumY + yi
                        val firstIdx = firstCellParticle[cellNr]
                        val lastIdx = firstCellParticle[cellNr + 1]

                        for (j in firstIdx until lastIdx) {
                            val id = cellParticleIds[j]
                            if (id == i) continue
                            val qx = particlePos[2 * id]
                            val qy = particlePos[2 * id + 1]

                            var dx = qx - px
                            var dy = qy - py
                            val d2 = dx * dx + dy * dy
                            if (d2 > minDist2 || d2 == 0.0) continue
                            val d = sqrt(d2)
                            val s = 0.5f * (minDist - d) / d
                            dx *= s
                            dy *= s
                            particlePos[2 * i] -= dx
                            particlePos[2 * i + 1] -= dy
                            particlePos[2 * id] += dx
                            particlePos[2 * id + 1] += dy

                            for (k in 0..2) {
                                val c0 = particleColor[3 * i + k]
                                val c1 = particleColor[3 * id + k]
                                val color = (c0 + c1) * 0.5f
                                particleColor[3 * i + k] = c0 + (color - c0) * colorDiffusionCoeff
                                particleColor[3 * id + k] = c1 + (color - c1) * colorDiffusionCoeff
                            }
                        }
                    }
                }
            }
        }
    }

    fun handleParticleCollisions(obstacleX: Double, obstacleY: Double, obstacleRadius: Double) {
        val h = 1.0 / fInvSpacing
        val r = particleRadius
        val or2 = obstacleRadius * obstacleRadius
        val minDist = obstacleRadius + r
        val minDist2 = minDist * minDist
        val minX = h + r
        val maxX = (fNumX - 1) * h - r
        val minY = h + r
        val maxY = (fNumY - 1) * h - r

        for (i in 0 until numParticles) {
            var x = particlePos[2 * i]
            var y = particlePos[2 * i + 1]
            val dx = x - obstacleX
            val dy = y - obstacleY
            val d2 = dx * dx + dy * dy

            if (d2 < minDist2) {
                // Set to obstacle velocity (stub: override with actual scene)
                particleVel[2 * i] = 0.0 // scene.obstacleVelX
                particleVel[2 * i + 1] = 0.0 // scene.obstacleVelY
            }

            if (x < minX) {
                x = minX; particleVel[2 * i] = 0.0
            }
            if (x > maxX) {
                x = maxX; particleVel[2 * i] = 0.0
            }
            if (y < minY) {
                y = minY; particleVel[2 * i + 1] = 0.0
            }
            if (y > maxY) {
                y = maxY; particleVel[2 * i + 1] = 0.0
            }

            particlePos[2 * i] = x
            particlePos[2 * i + 1] = y
        }
    }

    fun updateParticleDensity() {
        val n = fNumY
        val h1 = fInvSpacing
        val h2 = 0.5f * h
        val d = particleDensity
        d.fill(0.0)

        for (i in 0 until numParticles) {
            var x = particlePos[2 * i]
            var y = particlePos[2 * i + 1]

            x = clamp(x, h, (fNumX - 1) * h)
            y = clamp(y, h, (fNumY - 1) * h)

            val x0 = floor((x - h2) * h1).toInt()
            val tx = ((x - h2) - x0 * h) * h1
            val x1 = min(x0 + 1, fNumX - 2)

            val y0 = floor((y - h2) * h1).toInt()
            val ty = ((y - h2) - y0 * h) * h1
            val y1 = min(y0 + 1, fNumY - 2)

            val sx = 1.0 - tx
            val sy = 1.0 - ty

            if (x0 < fNumX && y0 < fNumY) d[x0 * n + y0] += sx * sy
            if (x1 < fNumX && y0 < fNumY) d[x1 * n + y0] += tx * sy
            if (x1 < fNumX && y1 < fNumY) d[x1 * n + y1] += tx * ty
            if (x0 < fNumX && y1 < fNumY) d[x0 * n + y1] += sx * ty
        }

        if (particleRestDensity == 0.0) {
            var sum = 0.0
            var count = 0
            for (i in 0 until fNumCells) {
                if (cellType[i] == FLUID_CELL) {
                    sum += d[i]
                    count++
                }
            }
            if (count > 0) {
                particleRestDensity = sum / count
            }
        }
    }

    fun transferVelocities(toGrid: Boolean, flipRatio: Double = 0.0) {
        val n = fNumY
        val h1 = fInvSpacing
        val h2 = 0.5f * h

        if (toGrid) {
            prevU.copyInto(u)
            prevV.copyInto(v)

            du.fill(0.0)
            dv.fill(0.0)
            u.fill(0.0)
            v.fill(0.0)

            for (i in 0 until fNumCells) {
                cellType[i] = if (s[i] == 0.0) SOLID_CELL else AIR_CELL
            }

            for (i in 0 until numParticles) {
                val x = particlePos[2 * i]
                val y = particlePos[2 * i + 1]
                val xi = clamp(floor(x * h1).toInt(), 0, fNumX - 1)
                val yi = clamp(floor(y * h1).toInt(), 0, fNumY - 1)
                val cellNr = xi * n + yi
                if (cellType[cellNr] == AIR_CELL) cellType[cellNr] = FLUID_CELL
            }
        }

        for (component in 0..1) {
            val dx = if (component == 0) 0.0 else h2
            val dy = if (component == 0) h2 else 0.0
            val f = if (component == 0) u else v
            val prevF = if (component == 0) prevU else prevV
            val d = if (component == 0) du else dv

            for (i in 0 until numParticles) {
                var x = clamp(particlePos[2 * i], h, (fNumX - 1) * h)
                var y = clamp(particlePos[2 * i + 1], h, (fNumY - 1) * h)

                val x0 = min(floor((x - dx) * h1).toInt(), fNumX - 2)
                val tx = ((x - dx) - x0 * h) * h1
                val x1 = min(x0 + 1, fNumX - 2)

                val y0 = min(floor((y - dy) * h1).toInt(), fNumY - 2)
                val ty = ((y - dy) - y0 * h) * h1
                val y1 = min(y0 + 1, fNumY - 2)

                val sx = 1.0 - tx
                val sy = 1.0 - ty

                val d0 = sx * sy
                val d1 = tx * sy
                val d2 = tx * ty
                val d3 = sx * ty

                val nr0 = x0 * n + y0
                val nr1 = x1 * n + y0
                val nr2 = x1 * n + y1
                val nr3 = x0 * n + y1

                if (toGrid) {
                    val pv = particleVel[2 * i + component]
                    f[nr0] += pv * d0; d[nr0] += d0
                    f[nr1] += pv * d1; d[nr1] += d1
                    f[nr2] += pv * d2; d[nr2] += d2
                    f[nr3] += pv * d3; d[nr3] += d3
                } else {
                    val offset = if (component == 0) n else 1
                    val valid0 =
                        if (cellType[nr0] != AIR_CELL || cellType[nr0 - offset] != AIR_CELL) 1.0 else 0.0
                    val valid1 =
                        if (cellType[nr1] != AIR_CELL || cellType[nr1 - offset] != AIR_CELL) 1.0 else 0.0
                    val valid2 =
                        if (cellType[nr2] != AIR_CELL || cellType[nr2 - offset] != AIR_CELL) 1.0 else 0.0
                    val valid3 =
                        if (cellType[nr3] != AIR_CELL || cellType[nr3 - offset] != AIR_CELL) 1.0 else 0.0

                    val v0 = particleVel[2 * i + component]
                    val dSum = valid0 * d0 + valid1 * d1 + valid2 * d2 + valid3 * d3

                    if (dSum > 0.0) {
                        val picV =
                            (valid0 * d0 * f[nr0] + valid1 * d1 * f[nr1] + valid2 * d2 * f[nr2] + valid3 * d3 * f[nr3]) / dSum
                        val corr =
                            (valid0 * d0 * (f[nr0] - prevF[nr0]) + valid1 * d1 * (f[nr1] - prevF[nr1]) +
                                    valid2 * d2 * (f[nr2] - prevF[nr2]) + valid3 * d3 * (f[nr3] - prevF[nr3])) / dSum
                        val flipV = v0 + corr
                        particleVel[2 * i + component] =
                            (1.0 - flipRatio) * picV + flipRatio * flipV
                    }
                }
            }

            if (toGrid) {
                for (i in f.indices) {
                    if (d[i] > 0.0) f[i] /= d[i]
                }

                for (i in 0 until fNumX) {
                    for (j in 0 until fNumY) {
                        val idx = i * n + j
                        val solid = (cellType[idx] == SOLID_CELL)
                        if (solid || (i > 0 && cellType[(i - 1) * n + j] == SOLID_CELL)) {
                            u[idx] = prevU[idx]
                        }
                        if (solid || (j > 0 && cellType[i * n + j - 1] == SOLID_CELL)) {
                            v[idx] = prevV[idx]
                        }
                    }
                }
            }
        }
    }

    fun solveIncompressibility(
        numIters: Int,
        dt: Double,
        overRelaxation: Double,
        compensateDrift: Boolean = true
    ) {
        p.fill(0.0)
        prevU.copyInto(u)
        prevV.copyInto(v)

        val n = fNumY
        val cp = density * h / dt

        repeat(numIters) {
            for (i in 1 until fNumX - 1) {
                for (j in 1 until fNumY - 1) {
                    val center = i * n + j
                    if (cellType[center] != FLUID_CELL) continue

                    val left = (i - 1) * n + j
                    val right = (i + 1) * n + j
                    val bottom = i * n + j - 1
                    val top = i * n + j + 1

                    val sx0 = s[left]
                    val sx1 = s[right]
                    val sy0 = s[bottom]
                    val sy1 = s[top]
                    val sTotal = sx0 + sx1 + sy0 + sy1

                    if (sTotal == 0.0) continue

                    var div = u[right] - u[center] + v[top] - v[center]

                    if (particleRestDensity > 0.0 && compensateDrift) {
                        val compression = particleDensity[center] - particleRestDensity
                        if (compression > 0.0) {
                            div -= compression
                        }
                    }

                    var dp = -div / sTotal
                    dp *= overRelaxation
                    p[center] += cp * dp

                    u[center] -= sx0 * dp
                    u[right] += sx1 * dp
                    v[center] -= sy0 * dp
                    v[top] += sy1 * dp
                }
            }
        }
    }

    fun updateParticleColors() {
        val h1 = fInvSpacing
        for (i in 0 until numParticles) {
            val s = 0.01f
            val idx = 3 * i
            particleColor[idx] = clamp(particleColor[idx] - s, 0.0, 1.0)
            particleColor[idx + 1] = clamp(particleColor[idx + 1] - s, 0.0, 1.0)
            particleColor[idx + 2] = clamp(particleColor[idx + 2] + s, 0.0, 1.0)

            val x = particlePos[2 * i]
            val y = particlePos[2 * i + 1]
            val xi = clamp(floor(x * h1).toInt(), 1, fNumX - 1)
            val yi = clamp(floor(y * h1).toInt(), 1, fNumY - 1)
            val cellNr = xi * fNumY + yi

            val d0 = particleRestDensity
            if (d0 > 0.0) {
                val relDensity = particleDensity[cellNr] / d0
                if (relDensity < 0.7f) {
                    particleColor[idx] = 0.8
                    particleColor[idx + 1] = 0.8
                    particleColor[idx + 2] = 1.0
                }
            }
        }
    }

    fun setSciColor(cellNr: Int, value: Double, minVal: Double, maxVal: Double) {
        val v = clamp(value, minVal, maxVal - 0.0001f)
        val d = maxVal - minVal
        val norm = if (d == 0.0) 0.5 else (v - minVal) / d
        val m = 0.25f
        val num = floor(norm / m).toInt()
        val s = (norm - num * m) / m

        val (r, g, b) = when (num) {
            0 -> Triple(0.0, s, 1.0)
            1 -> Triple(0.0, 1.0, 1.0 - s)
            2 -> Triple(s, 1.0, 0.0)
            else -> Triple(1.0, 1.0 - s, 0.0)
        }

        val idx = 3 * cellNr
        cellColor[idx] = r
        cellColor[idx + 1] = g
        cellColor[idx + 2] = b
    }

    fun updateCellColors() {
        cellColor.fill(0.0)
        for (i in 0 until fNumCells) {
            val idx = 3 * i
            if (cellType[i] == SOLID_CELL) {
                cellColor[idx] = 0.5
                cellColor[idx + 1] = 0.5
                cellColor[idx + 2] = 0.5
            } else if (cellType[i] == FLUID_CELL) {
                var d = particleDensity[i]
                if (particleRestDensity > 0.0) {
                    d /= particleRestDensity
                }
                setSciColor(i, d, 0.0, 2.0)
            }
        }
    }

    fun simulate(
        dt: Double,
        gravityX: Double,
        gravityY: Double,
        flipRatio: Double,
        numPressureIters: Int,
        numParticleIters: Int,
        overRelaxation: Double,
        compensateDrift: Boolean,
        separateParticles: Boolean,
        obstacleX: Double,
        obstacleY: Double,
        obstacleRadius: Double
    ) {
        val numSubSteps = 1
        val sdt = dt / numSubSteps

        repeat(numSubSteps) {
            integrateParticles(sdt, gravityX, gravityY)
            if (separateParticles) pushParticlesApart(numParticleIters)
            handleParticleCollisions(obstacleX, obstacleY, obstacleRadius)
            transferVelocities(true)
            updateParticleDensity()
            solveIncompressibility(numPressureIters, sdt, overRelaxation, compensateDrift)
            transferVelocities(false, flipRatio)
        }

        updateParticleColors()
        updateCellColors()
    }


    companion object {
        const val FLUID_CELL = 1
        const val SOLID_CELL = 2
        const val AIR_CELL = 0

        fun clamp(x: Int, min: Int, max: Int): Int = x.coerceIn(min, max)
        fun clamp(x: Double, min: Double, max: Double): Double = x.coerceIn(min, max)
    }

}
