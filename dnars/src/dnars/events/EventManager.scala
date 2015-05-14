package dnars.events

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.locks.ReentrantLock
import scala.collection.mutable.ListBuffer
import dnars.graph.DNarsGraphAPI

trait EventManager extends DNarsGraphAPI {
	//	private val list = new ListBuffer[EventPayload]
	//	private val observers = new ListBuffer[String]()
	//	private var _paused: Boolean = false
	//
	//	val NUM_DISPATCHERS = 1
	//	for (i <- 0 until NUM_DISPATCHERS)
	//		new EDT(list, observers).start

	def addEvent(event: EventPayload): Unit = {
		println(event.event + ":::" + event.statement)
		//		list synchronized {
		//			list += event
		//			if (!paused)
		//				list.notify
		//		}
	}

	def addObserver(aid: String): Unit = {
		//		observers synchronized { observers += aid }
	}

	def removeObserver(aid: String): Unit = {
		//		observers synchronized { observers -= aid }
	}

	def paused = {
		true
		//	this synchronized {	
		//		//		_paused
		//	}
	}

	def paused_=(value: Boolean): Unit = {
		//		this synchronized {
		//			_paused = value
		//			if (!value)
		//				list synchronized {
		//					if (list.size > 0)
		//						list.notify
		//				}
		//		}
	}
}