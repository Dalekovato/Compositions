package com.example.compositions.presentation

import android.app.Application
import android.content.Context
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.compositions.R
import com.example.compositions.data.GameRepositoryImpl
import com.example.compositions.domain.entity.GameResult
import com.example.compositions.domain.entity.GameSettings
import com.example.compositions.domain.entity.Level
import com.example.compositions.domain.entity.Question
import com.example.compositions.domain.usecase.GenerateQuestionUseCase
import com.example.compositions.domain.usecase.GetGameSettingsUseCase

class GameViewModel(
    private val application: Application ,
    private val level: Level
) : ViewModel(){

    private lateinit var gameSettings: GameSettings


    private val repository = GameRepositoryImpl

    private val generateQuestionUseCase = GenerateQuestionUseCase(repository)
    private val getGameSettingsUseCase = GetGameSettingsUseCase(repository)

    private var timer: CountDownTimer? = null

    private val _formattedTime = MutableLiveData<String>()
    val formattedTime: LiveData<String>
        get() = _formattedTime

    private  val _question = MutableLiveData<Question>()
    val question : MutableLiveData<Question>
        get() = _question

    private val _percentOfRightAnswers = MutableLiveData<Int>()
    val percentOfRightAnswers : MutableLiveData<Int>
        get() = _percentOfRightAnswers

    private val _progressAnswers = MutableLiveData<String>()
    val progressAnswers : MutableLiveData<String>
        get() = _progressAnswers

    private val _enoughtCountOfRightAnswer = MutableLiveData<Boolean>()
    val enoughtCountOfRightAnswer : MutableLiveData<Boolean>
        get() = _enoughtCountOfRightAnswer

    private val _enoughtPercentOfRightAnswer = MutableLiveData<Boolean>()
    val enoughtPercentOfRightAnswer : MutableLiveData<Boolean>
        get() = _enoughtPercentOfRightAnswer

    private val _minPercent = MutableLiveData<Int>()
    val minPercent: MutableLiveData<Int>
        get() = _minPercent

    private val _gameResult = MutableLiveData<GameResult>()
    val gameResult: MutableLiveData<GameResult>
        get() = _gameResult


    private var countOfRightAnswers = 0
    private var countOfQuestions = 0

    init {
        startGame()
    }

    private fun startGame() {
        getGameSettings()
        startTimer()
        generateQuestion()
        updateProgress()
    }

    fun chooseAnswer(number: Int){
        checkAnswer(number)
        updateProgress()
        generateQuestion()

    }

    private fun updateProgress (){
        val percent = calculatePercentOFRightAnswers()
        _percentOfRightAnswers.value = percent
        _progressAnswers.value = String.format(
            application.resources.getString(R.string.progress_answers),
            countOfRightAnswers,
            gameSettings.minCountOfRightAnswers
        )

        _enoughtCountOfRightAnswer.value = countOfRightAnswers >= gameSettings.minCountOfRightAnswers
        _enoughtPercentOfRightAnswer.value = percent>= gameSettings.minPercentOfRightAnswers

    }

    private fun calculatePercentOFRightAnswers():Int{
        if (countOfQuestions == 0){
            return 0
        }
        return ((countOfRightAnswers / countOfQuestions.toDouble())*100).toInt()
    }


    private fun checkAnswer(number: Int){
        val rightAnswer = question.value?.rightAnswer
        if (number==rightAnswer){
            countOfRightAnswers++
        }
        countOfQuestions++
    }

    private fun getGameSettings(){
        this.gameSettings = getGameSettingsUseCase(level)
        _minPercent.value = gameSettings.minPercentOfRightAnswers
    }

    private fun startTimer() {
         timer = object : CountDownTimer(
            gameSettings.gameTimeInSeconds * MILLIS_IN_SECONDS,
            MILLIS_IN_SECONDS
        ) {
            override fun onTick(millisUntilFinished: Long) {
                _formattedTime.value = formatTime(millisUntilFinished)
            }

            override fun onFinish() {
                finishGame()
            }

        }
        timer?.start()
    }

    private fun generateQuestion(){

       _question.value =  generateQuestionUseCase(gameSettings.maxSumValue)
    }

    private fun finishGame(){

        _gameResult.value = GameResult(
            enoughtCountOfRightAnswer.value == true && enoughtPercentOfRightAnswer.value == true ,
            countOfRightAnswers,
            countOfQuestions ,
            gameSettings
        )

    }

    override fun onCleared() {
        super.onCleared()
        timer?.cancel()
    }

    private fun formatTime(millisUntilFinished: Long): String{
        val seconds = millisUntilFinished / MILLIS_IN_SECONDS
        val minutes = seconds / SECONDS_IN_MINUTE
        val leftSeconds = seconds - (minutes * SECONDS_IN_MINUTE)

        return String.format("%02d:%02d", minutes , leftSeconds)

    }

    companion object {

        private const val MILLIS_IN_SECONDS = 1000L
        private const val SECONDS_IN_MINUTE = 60

    }

}