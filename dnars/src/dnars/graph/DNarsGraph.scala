/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 *
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */

package dnars.graph

import org.apache.commons.configuration.BaseConfiguration
import org.apache.commons.configuration.Configuration

import com.thinkaurelius.titan.core.Cardinality
import com.thinkaurelius.titan.core.Order
import com.thinkaurelius.titan.core.PropertyKey
import com.thinkaurelius.titan.core.TitanFactory
import com.thinkaurelius.titan.core.TitanGraph
import com.thinkaurelius.titan.core.schema.TitanManagement
import com.thinkaurelius.titan.core.util.TitanCleanup
import com.tinkerpop.blueprints.Direction
import com.tinkerpop.blueprints.Graph
import com.tinkerpop.blueprints.GraphQuery
import com.tinkerpop.blueprints.Vertex
import com.tinkerpop.gremlin.scala.ScalaGraph

import dnars.base.Copula
import dnars.base.Statement
import dnars.events.EventManager
import dnars.graph.Wrappers.edge2DNarsEdge
import dnars.graph.Wrappers.vertex2DNarsVertex
import dnars.inference.ResolutionEngine
import dnars.inference.forward.AbductionComparisonAnalogy
import dnars.inference.forward.AnalogyInv
import dnars.inference.forward.AnalogyResemblance
import dnars.inference.forward.DeductionAnalogy
import dnars.inference.forward.InductionComparison

/**
 * Wrapper around the ScalaGraph class. Inspired by
 * <a href="https://github.com/mpollmeier/gremlin-scala">gremlin/scala project</a>.
 *
 * @author <a href="mailto:mitrovic.dejan@gmail.com">Dejan Mitrovic</a>
 */
class DNarsGraph(override val graph: Graph, val domain: String) extends ScalaGraph(graph)
	with DNarsGraphAPI with VertexAPI with EdgeAPI with StatementAPI with EventManager
	with ResolutionEngine {

	val engines = List(
		new DeductionAnalogy(this),
		new AnalogyResemblance(this),
		new AbductionComparisonAnalogy(this),
		new InductionComparison(this),
		new AnalogyInv(this))

	override def conclusions(input: Array[Statement]): Array[Statement] = {
		val inputList = input.toList // for compatibility with Java
		val result = engines.flatMap { engine => engine.apply(inputList) }
		result.toArray
	}

	override def conclusions(input: Statement): Array[Statement] =
		conclusions(Array(input))

	override def include(input: Array[Statement]): Unit = {
		val concl = conclusions(input)
		add(input.toList ::: concl.toList)
	}

	override def query(): GraphQuery = graph.query()

	def shutdown(): Unit = graph.shutdown()

	def getVertex(id: Any): Vertex = graph.getVertex(id)

	def commit(): Unit = graph match {
		case tg: TitanGraph =>
			tg.commit()
		case any: Any =>
			throw new IllegalArgumentException(any.getClass.getName + " cannot be cleared")
	}

	def clear(): Unit = graph match {
		case tg: TitanGraph =>
			TitanCleanup.clear(tg)
		case any: Any =>
			throw new IllegalArgumentException(any.getClass.getName + " cannot be cleared")
	}

	/**
	 * Debugging purposes only.
	 */
	def printEdges(): Unit = {
		println(s"---------------- Graph dump [domain=$domain] ----------------")
		forEachStatement(println(_))
		println("------------------- Done -------------------")
	}

	/**
	 * Debugging purposes only.
	 */
	def forEachStatement(f: (Statement) => Unit): Unit = {
		V
		val allSt = E.map { e =>
			val s: DNarsVertex = e.getVertex(Direction.OUT)
			val p: DNarsVertex = e.getVertex(Direction.IN)
			val c = e.getLabel
			val t = e.truth
			val st = Statement(s.term, c, p.term, t)
			// print only the packed version
			st.pack() match {
				case List() => st
				case List(h, _) => h
			}
		}.toSet
		allSt.foreach(f)
	}

	/**
	 * Debugging purposes only.
	 */
	def getEdgeCount(): Long = {
		var count = 0L
		V.as("x").inE.sideEffect { e => count += 1 }.back("x").outE.sideEffect { e => count += 1 }.iterate
		count
	}
}

object DNarsGraph {
	def apply(graph: ScalaGraph, keyspace: String) = wrap(graph, keyspace)
	implicit def wrap(graph: ScalaGraph, keyspace: String) = new DNarsGraph(graph, keyspace)
	implicit def unwrap(wrapper: DNarsGraph) = wrapper.graph
}

object DNarsGraphFactory {
	val logger = java.util.logging.Logger.getLogger(classOf[DNarsGraph].getName)

	def create(domain: String, customConfig: java.util.Map[String, Any] = null): DNarsGraph = {
		val conf = getConfig(domain, customConfig)
		val graph = TitanFactory.open(conf)
		if (!graph.containsPropertyKey("term")) {
			logger.info("Initializing empty domain " + domain)
			GraphSchemaBuilder(graph)
		}
		DNarsGraph(ScalaGraph(graph), domain)
	}

	private def getConfig(domain: String, customConfig: java.util.Map[String, Any]): Configuration = {
		val conf = new BaseConfiguration
		conf.setProperty("storage.backend", "cassandra")
		conf.setProperty("storage.hostname", "localhost");
		conf.setProperty("storage.cassandra.keyspace", domain)
		includeCustomConfig(conf, customConfig)
	}

	private def includeCustomConfig(conf: BaseConfiguration, customConfig: java.util.Map[String, Any]): BaseConfiguration = {
		if (customConfig != null) {
			val i = customConfig.entrySet().iterator
			while (i.hasNext()) {
				val c = i.next()
				conf.setProperty(c.getKey(), c.getValue())
			}
		}
		conf
	}
}

object GraphSchemaBuilder {
	def apply(graph: TitanGraph): Unit = {
		addVertexKey(graph)
		addEdgeKeys(graph)
	}

	private def addVertexKey(graph: TitanGraph): Unit = {
		val management = graph.getManagementSystem
		val key = management.makePropertyKey("term").dataType(classOf[String]).make()
		management.buildIndex("byTerm", classOf[Vertex]).addKey(key).buildCompositeIndex()
		management.commit()
	}

	private def addEdgeKeys(graph: TitanGraph): Unit = {
		val management = graph.getManagementSystem
		graph.makePropertyKey("truth").dataType(classOf[String]).cardinality(Cardinality.SINGLE).make()
		addOrderedIndexes(graph, management)
		management.commit()
	}

	private def addOrderedIndexes(graph: TitanGraph, management: TitanManagement): Unit = {
		val keys = List(makePropertyKey(graph, "subjExp"), makePropertyKey(graph, "predExp"))
		val copulas = List(Copula.Inherit, Copula.Similar)
		copulas.foreach { c: String =>
			val label = management.makeEdgeLabel(c).make()
			keys.foreach { key: PropertyKey =>
				management.buildEdgeIndex(label, "by" + key.getName, Direction.BOTH, Order.DESC, key)
			}
		}
	}

	private def makePropertyKey(graph: TitanGraph, name: String): PropertyKey =
		graph.makePropertyKey(name).dataType(classOf[Integer]).cardinality(Cardinality.SINGLE).make()
}