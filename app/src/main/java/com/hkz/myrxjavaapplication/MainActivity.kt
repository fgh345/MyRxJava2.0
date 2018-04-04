package com.hkz.myrxjavaapplication

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        rxjava1()
//        rxjava2()
//        rxjava3()
//        rxjava4()
//        rxjava6()
        rxjava7()
    }

    /**
     * 最基本的
     * */
    fun rxjava1() {
        Observable.create(ObservableOnSubscribe<String> {
            //我是发射器(被观察者)
            Log.d("hkz", "我在ObservableOnSubscribe<String>我的线程是" + Thread.currentThread().name)
            it.onNext("我是第一个next!")
            it.onNext("我是第二个next!")
            it.onComplete()
        })
                .subscribeOn(Schedulers.io())//设置发射者线程 如果不设置此行 默认全是主线程 只设置此行(针对下行) 全是子线程  subscribeOn() 只有第一次的有效
                .observeOn(AndroidSchedulers.mainThread())//设置接收者线程 这里和上一行需要成对出现 才能实现 观察者与被观察者线程分开 observeOn()可以设置多次
                .subscribe(object : Observer<String> {
                    //我是接收者
                    override fun onComplete() {
                        Log.d("hkz", "onComplete 我的线程是" + Thread.currentThread().name)
                    }

                    override fun onSubscribe(d: Disposable) {
                        Log.d("hkz", "onSubscribe  我的线程是" + Thread.currentThread().name)
                    }

                    override fun onNext(t: String) {
                        Log.d("hkz", "Observer onNext 我的线程是" + Thread.currentThread().name)
                        Log.d("hkz", "Observer onNext 我的返回值是 $t")
                    }

                    override fun onError(e: Throwable) {
                        Log.d("hkz", "onError 我的线程是" + Thread.currentThread().name)
                    }
                })
    }

    /**
     * 理解 doOnNext Consumer
     * */
    fun rxjava2() {
        Observable.create(ObservableOnSubscribe<String> {
            //我是发射者
            Log.d("hkz", "我在ObservableOnSubscribe<String>我的线程是" + Thread.currentThread().name)
            it.onNext("我是第一个next!")
            it.onNext("我是第二个next!")
            it.onComplete()
        })
                .subscribeOn(Schedulers.io())//设置发射者线程 如果不设置此行 默认全是主线程 只设置此行(针对下行) 全是子线程  subscribeOn() 只有第一次的有效
                .observeOn(Schedulers.io())//设置接收者线程 这里和上一行需要成对出现 才能实现 观察者与被观察者线程分开 observeOn()可以设置多次
                .doOnNext {
                    //doOnNext 是一个 在观察者 和 发射器 运行流程之间的方法 线程可自由切换
                    Log.d("hkz", "doOnNext我应该是子线程 我的线程是" + Thread.currentThread().name)
                    Log.d("hkz", "我在doOnNext 我的返回值是 $it")
                }
                .observeOn(AndroidSchedulers.mainThread())//第二次设置  这里影响Consumer线程

                /**
                 * Consumer是简易版的Observer，他有多重重载，可以自定义你需要处理的信息，我这里调用的是只接受onNext消息的方法，
                 * 他只提供一个回调接口accept，由于没有onError和onCompete，无法再 接受到onError或者onCompete之后，实现函数回调。
                 * 无法回调，并不代表不接收，他还是会接收到onCompete和onError之后做出默认操作，也就是监听者（Consumer）不在接收
                 * Observable发送的消息，下方的代码测试了该效果。
                 */
                .subscribe({
                    //我是接收者
                    Log.d("hkz", "我在Consumer 我的线程是" + Thread.currentThread().name)
                    Log.d("hkz", "我在Consumer 我的返回值是 $it")
                })

    }

    /**
     * 理解 map
     * map可以重新加工数据
     * */
    fun rxjava3() {
        Observable.create(ObservableOnSubscribe<String> {
            //我是发射者
            Log.d("hkz", "我在ObservableOnSubscribe<String>我的线程是" + Thread.currentThread().name)
            it.onNext("我是第一个next!")
            it.onNext("我是第二个next!")
            it.onComplete()
        })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())//这里影响map线程
//                .observeOn(AndroidSchedulers.mainThread())// 这里影响map线程
                .map {
                    Log.d("hkz", "我在map 我的线程是" + Thread.currentThread().name)
                    if ("我是第一个next!" == it)
                        return@map "我是第一个经过map的next!"
                    else
                        return@map "我是第二个经过map的next!"
                }
                .observeOn(AndroidSchedulers.mainThread())// 设置Consumer线程
                .subscribe({
                    //我是接收者
                    Log.d("hkz", "我在Consumer 我的线程是" + Thread.currentThread().name)
                    Log.d("hkz", "我在Consumer 我的返回值是 $it")
                })
    }


    /**
     * 理解 concat
     * concat 可以做到不交错的发射两个甚至多个 Observable 的发射事件，并且只有前一个 Observable 终止(onComplete) 后才会订阅下一个 Observable
     * */
    fun rxjava4() {
        //发射器一号
        val localwork = Observable.create(ObservableOnSubscribe<String> {
            it.onNext("我是localwork!我假装在这里完成本地数据操作")
            it.onComplete()//只有执行完这里 才会执行network
        })
        //发射器二号
        val network = Observable.create(ObservableOnSubscribe<String> {
            it.onNext("我是network!我假装在这里完成网络数据操作")
            it.onComplete()
        })
        //发射器三号
        val network2 = Observable.create(ObservableOnSubscribe<String> {
            it.onNext("我是发射器三号")
        })

        Observable.concat(localwork, network, network2)
                .subscribeOn(Schedulers.io())//设置发射者线程 如果不设置此行 默认全是主线程 只设置此行(针对下行) 全是子线程  subscribeOn() 只有第一次的有效
                .observeOn(Schedulers.io())//设置接收者线程 这里和上一行需要成对出现 才能实现 观察者与被观察者线程分开 observeOn()可以设置多次
                .subscribe({
                    //我是接收者
                    Log.d("hkz", "我在Consumer 我的返回值是 $it")
                })
    }

    /**
     * 理解 flatMap
     * flatMap 操作符可以将一个发射数据的 Observable 变换为多个 Observables ，然后将它们发射的数据合并后放到一个单独的 Observable，利用这个特性，我们很轻松地达到了我们的需求。
     *
     * 这个 我没搞懂....例子有问题 大家不用看
     * */
    fun rxjava5() {
//        Observable.create(ObservableOnSubscribe<String> {
//            //我是发射器(被观察者)
//            //我假装第一次联网获取的数据1
//            it.onNext("1")
//        })
//                .subscribeOn(Schedulers.io())
//                .observeOn(Schedulers.io())
//                .flatMap(Function<String, ObservableSource<String>> {
//                    return@Function Observable.create({
//                        //我假装获取到了联网数据1,然后接着联网获取2
//                        it.onNext("${it}2")
//                        it.onComplete()
//                    })
//
//                })
//                .subscribe {
//                    Log.d("hkz", "我假装经过了两次联网 我的返回值是 $it")
//                }

    }

    /**
     * 理解 zip
     * zip 操作符可以将多个 Observable 的数据结合为一个数据源再发射出去
     * */
    fun rxjava6() {
        val observable1 = Observable.create(ObservableOnSubscribe<String> {
            //我是发射者
            Log.d("hkz", "observable1")
            it.onNext("我是第一个发射器!")
            it.onComplete()
        })

        val observable2 = Observable.create(ObservableOnSubscribe<String> {
            //我是发射者
            Log.d("hkz", "observable2")
            it.onNext("我是第二个发射器!")
            it.onComplete()
        })
        Observable.zip(observable1, observable2, BiFunction<String, String, String> { t1, t2 -> "$t1---$t2" })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    //我是接收者
                    Log.d("hkz", "我在Consumer 我的返回值是$it")
                })
    }

    private var mDisposable: Disposable? = null
    /**
     * 完成一个心态任务
     * */
    fun rxjava7() {

        mDisposable = Flowable.interval(5, TimeUnit.SECONDS)
//                .observeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.d("hkz", "心跳!$it 我的线程是" + Thread.currentThread().name)
                })

    }

    override fun onDestroy() {
        super.onDestroy()
        mDisposable?.dispose()
    }
}
