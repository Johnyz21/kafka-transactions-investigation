import scala.util.{Failure, Success, Try}

object UserInputHandler {
  def getUserInput[A]( f: () => A ): A = {
    val userInput = Try{
      f()
    }
    userInput match {
      case Success(answer) => answer
      case Failure(e) => {
        println(e)
        getUserInput(f)
      }
    }
  }
}
