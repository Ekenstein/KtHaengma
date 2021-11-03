package haengma.core.utils

typealias Point = Pair<Int, Int>

data class Rectangle(
    val upperLeftCorner: Point,
    val lowerRightCorner: Point
) {
    init {
        require(upperLeftCorner.x <= lowerRightCorner.x)
        require(lowerRightCorner.y >= upperLeftCorner.y)
    }
}

val Point.x
    get() = first

val Point.y
    get() = second

val Rectangle.width
    get() = lowerRightCorner.x - upperLeftCorner.x

val Rectangle.height
    get() = lowerRightCorner.y - upperLeftCorner.y

val Rectangle.asPoints: Set<Point>
    get() {
        val points = (0..width).mapIndexed { xIndex, _ ->
            (0..height).mapIndexed { yIndex, _ ->
                upperLeftCorner.x + xIndex to upperLeftCorner.y + yIndex
            }
        }

        return points.flatten().toSet()
    }