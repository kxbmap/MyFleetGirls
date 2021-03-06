package models.db

import com.ponkotuy.data
import scalikejdbc._

case class AGOProgress(
    memberId: Long,
    sortie: Int,
    rankS: Int,
    reachBoss: Int,
    winBoss: Int){
  import models.db.{AGOClear => C}

  def save()(implicit session: DBSession = AGOProgress.autoSession): Unit = AGOProgress.save(this)(session)

  def isClear: Boolean = C.sortie <= sortie && C.rankS <= rankS && C.reachBoss <= reachBoss && C.winBoss <= winBoss
}

object AGOProgress extends SQLSyntaxSupport[AGOProgress] {

  override val tableName = "ago_progress"

  override val columns = Seq("member_id", "sortie", "rank_s", "reach_boss", "win_boss")

  val AgoQuest = 214

  val ap = AGOProgress.syntax("ap")

  def apply(x: SyntaxProvider[AGOProgress])(rs: WrappedResultSet): AGOProgress = apply(x.resultName)(rs)
  def apply(x: ResultName[AGOProgress])(rs: WrappedResultSet): AGOProgress = autoConstruct(rs, x)

  def find(memberId: Long)(implicit session: DBSession = autoSession): Option[AGOProgress] = withSQL {
    select.from(AGOProgress as ap).where.eq(ap.memberId, memberId)
  }.map(AGOProgress(ap)).single().apply()

  private def agoState(memberId: Long): Int = Quest.find(AgoQuest, memberId).map(_.state).getOrElse(0)

  def incSortie(memberId: Long)(implicit session: DBSession = autoSession): Unit = {
    if(agoState(memberId) >= 2) {
      val ago = find(memberId).getOrElse(start(memberId))
      ago.copy(sortie = ago.sortie + 1).save()
    }
  }

  def incWithBattle(memberId: Long, battleResult: data.BattleResult, mapStart: data.MapStart)(implicit session: DBSession = autoSession): Unit = {
    if(agoState(memberId) >= 2) {
      val ago = find(memberId).getOrElse(start(memberId))
      val cellInfo = CellInfo.find(mapStart.mapAreaId, mapStart.mapInfoNo, mapStart.no)
      val boss = cellInfo.exists(_.boss)
      val reachBoss = ago.reachBoss + cond(boss)
      val winBoss = ago.winBoss + cond(boss && battleResult.win)
      val rankS = ago.rankS + cond(battleResult.winRank == "S")
      new AGOProgress(memberId, ago.sortie, rankS, reachBoss, winBoss).save()
    }
  }

  private def cond(b: Boolean): Int = if(b) 1 else 0

  private def save(p: AGOProgress)(implicit session: DBSession = autoSession): Unit = {
    val params = Seq(p.memberId, p.sortie, p.rankS, p.reachBoss, p.winBoss).map(x => sqls"$x")
    sql"replace into ago_progress value (${sqls.csv(params:_*)})".execute().apply()
  }

  def deleteAll()(implicit session: DBSession = autoSession) = withSQL {
    delete.from(AGOProgress)
  }.update().apply()

  def start(memberId: Long) = AGOProgress(memberId, 0, 0, 0, 0)
}

object AGOClear {
  val sortie = 36
  val rankS = 6
  val reachBoss = 24
  val winBoss = 12
}
