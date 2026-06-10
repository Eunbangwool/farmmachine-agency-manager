package com.sangwolnongsan.nongdori.data

import com.google.firebase.firestore.DocumentSnapshot
import com.sangwolnongsan.nongdori.shared.data.CatalogPart
import com.sangwolnongsan.nongdori.shared.data.Customer
import com.sangwolnongsan.nongdori.shared.data.MachineType
import com.sangwolnongsan.nongdori.shared.data.MaintenanceType
import com.sangwolnongsan.nongdori.shared.data.PartUsage
import com.sangwolnongsan.nongdori.shared.data.Priority
import com.sangwolnongsan.nongdori.shared.data.RepairRecord
import com.sangwolnongsan.nongdori.shared.data.RepairStatus
import com.sangwolnongsan.nongdori.shared.data.WorkOrder

/**
 * Firestore 문서 ↔ 도메인 모델 수동 매퍼.
 * 웹(window.nongdori, kotlinx.serialization)이 쓴 구조와 동일한 필드명을 사용한다.
 * 숫자는 getLong/getDouble 로 읽어 int/double 저장 차이를 흡수.
 */
object FirestoreMappers {

    // ── 읽기 ──
    fun workOrderFromDoc(doc: DocumentSnapshot): WorkOrder {
        val repairMap = doc.get("repair") as? Map<*, *>
        return WorkOrder(
            id = doc.id,
            orderNo = doc.getString("orderNo") ?: "",
            customerId = doc.getString("customerId") ?: "",
            customerName = doc.getString("customerName") ?: "",
            customerPhone = doc.getString("customerPhone") ?: "",
            customerAddress = doc.getString("customerAddress") ?: "",
            lat = doc.getDouble("lat"),
            lng = doc.getDouble("lng"),
            machineId = doc.getString("machineId"),
            machineName = doc.getString("machineName") ?: "",
            machineType = enumOrNull<MachineType>(doc.getString("machineType")),
            symptom = doc.getString("symptom") ?: "",
            priority = enumOrNull<Priority>(doc.getString("priority")) ?: Priority.NORMAL,
            status = enumOrNull<RepairStatus>(doc.getString("status")) ?: RepairStatus.RECEIVED,
            assignedEngineerUid = doc.getString("assignedEngineerUid"),
            assignedEngineerName = doc.getString("assignedEngineerName") ?: "",
            requestedAtMillis = doc.getLong("requestedAtMillis") ?: 0L,
            scheduledAtMillis = doc.getLong("scheduledAtMillis"),
            dispatchedAtMillis = doc.getLong("dispatchedAtMillis"),
            arrivedAtMillis = doc.getLong("arrivedAtMillis"),
            startedAtMillis = doc.getLong("startedAtMillis"),
            completedAtMillis = doc.getLong("completedAtMillis"),
            repair = repairFromMap(repairMap),
            customerSignatureUrl = doc.getString("customerSignatureUrl"),
            createdByUid = doc.getString("createdByUid") ?: "",
            createdAtMillis = doc.getLong("createdAtMillis") ?: 0L,
            updatedAtMillis = doc.getLong("updatedAtMillis") ?: 0L,
        )
    }

    fun customerFromDoc(doc: DocumentSnapshot): Customer = Customer(
        id = doc.id,
        name = doc.getString("name") ?: "",
        phone = doc.getString("phone") ?: "",
        address = doc.getString("address") ?: "",
        addressDetail = doc.getString("addressDetail") ?: "",
        lat = doc.getDouble("lat"),
        lng = doc.getDouble("lng"),
        businessName = doc.getString("businessName"),
        memo = doc.getString("memo") ?: "",
        createdAtMillis = doc.getLong("createdAtMillis") ?: 0L,
        updatedAtMillis = doc.getLong("updatedAtMillis") ?: 0L,
    )

    private fun repairFromMap(m: Map<*, *>?): RepairRecord {
        if (m == null) return RepairRecord()
        val parts = (m["partsUsed"] as? List<*>)?.mapNotNull { p ->
            (p as? Map<*, *>)?.let {
                PartUsage(
                    partId = it["partId"] as? String,
                    name = it["name"] as? String ?: "",
                    qty = numD(it["qty"]) ?: 1.0,
                    unitPrice = numI(it["unitPrice"]),
                    lineTotal = numI(it["lineTotal"]),
                )
            }
        } ?: emptyList()
        return RepairRecord(
            type = enumOrNull<MaintenanceType>(m["type"] as? String) ?: MaintenanceType.REPAIR,
            diagnosis = m["diagnosis"] as? String ?: "",
            workDescription = m["workDescription"] as? String ?: "",
            partsUsed = parts,
            laborHours = numD(m["laborHours"]),
            laborCost = numI(m["laborCost"]),
            partsCost = numI(m["partsCost"]),
            totalCost = numI(m["totalCost"]),
            operatingHoursAtService = numD(m["operatingHoursAtService"]),
            photoUrls = (m["photoUrls"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            performedByUid = m["performedByUid"] as? String ?: "",
            performedByName = m["performedByName"] as? String ?: "",
            isInProgress = m["isInProgress"] as? Boolean ?: true,
        )
    }

    // ── 쓰기 ──
    fun workOrderToMap(wo: WorkOrder): Map<String, Any?> = mapOf(
        "orderNo" to wo.orderNo,
        "customerId" to wo.customerId,
        "customerName" to wo.customerName,
        "customerPhone" to wo.customerPhone,
        "customerAddress" to wo.customerAddress,
        "lat" to wo.lat,
        "lng" to wo.lng,
        "machineId" to wo.machineId,
        "machineName" to wo.machineName,
        "machineType" to wo.machineType?.name,
        "symptom" to wo.symptom,
        "priority" to wo.priority.name,
        "status" to wo.status.name,
        "assignedEngineerUid" to wo.assignedEngineerUid,
        "assignedEngineerName" to wo.assignedEngineerName,
        "requestedAtMillis" to wo.requestedAtMillis,
        "scheduledAtMillis" to wo.scheduledAtMillis,
        "dispatchedAtMillis" to wo.dispatchedAtMillis,
        "arrivedAtMillis" to wo.arrivedAtMillis,
        "startedAtMillis" to wo.startedAtMillis,
        "completedAtMillis" to wo.completedAtMillis,
        "repair" to repairToMap(wo.repair),
        "customerSignatureUrl" to wo.customerSignatureUrl,
        "createdByUid" to wo.createdByUid,
        "createdAtMillis" to wo.createdAtMillis,
        "updatedAtMillis" to wo.updatedAtMillis,
    )

    private fun repairToMap(r: RepairRecord): Map<String, Any?> = mapOf(
        "type" to r.type.name,
        "diagnosis" to r.diagnosis,
        "workDescription" to r.workDescription,
        "partsUsed" to r.partsUsed.map {
            mapOf("partId" to it.partId, "name" to it.name, "qty" to it.qty, "unitPrice" to it.unitPrice, "lineTotal" to it.lineTotal)
        },
        "laborHours" to r.laborHours,
        "laborCost" to r.laborCost,
        "partsCost" to r.partsCost,
        "totalCost" to r.totalCost,
        "operatingHoursAtService" to r.operatingHoursAtService,
        "photoUrls" to r.photoUrls,
        "performedByUid" to r.performedByUid,
        "performedByName" to r.performedByName,
        "isInProgress" to r.isInProgress,
    )

    // ── 기종별 부품 카탈로그 ──
    fun catalogPartFromDoc(doc: DocumentSnapshot): CatalogPart = CatalogPart(
        id = doc.id,
        machineType = enumOrNull<MachineType>(doc.getString("machineType")) ?: MachineType.OTHER,
        customTypeName = doc.getString("customTypeName"),
        modelName = doc.getString("modelName") ?: "",
        partName = doc.getString("partName") ?: "",
        partNumber = doc.getString("partNumber") ?: "",
        note = doc.getString("note") ?: "",
        unitPrice = numI(doc.get("unitPrice")),
    )

    fun catalogPartToMap(e: CatalogPart): Map<String, Any?> = mapOf(
        "id" to e.id,
        "machineType" to e.machineType.name,
        "customTypeName" to e.customTypeName,
        "modelName" to e.modelName,
        "partName" to e.partName,
        "partNumber" to e.partNumber,
        "note" to e.note,
        "unitPrice" to e.unitPrice,
    )

    private fun numD(v: Any?): Double? = (v as? Number)?.toDouble()
    private fun numI(v: Any?): Int? = (v as? Number)?.toInt()

    private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
        name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }
}
