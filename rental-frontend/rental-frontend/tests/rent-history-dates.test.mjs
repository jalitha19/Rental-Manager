import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildRentSchedule, appendHistoricalPeriod, displayDate, endOfMonthIso,
  parseDisplayDate, scheduleToForm, validateRentHistory,
} from '../src/utils/rentPeriods.js'
import { prefillCurrentOccupants, toOccupantPayload, emptyOccupant } from '../src/utils/occupants.js'

const history = [
  { startDate: '2018-01-12', endDate: '2022-07-18', monthlyRent: '3500' },
  { startDate: '2022-08-09', endDate: '2025-10-21', monthlyRent: '5000' },
]
const config = { stayStart: '2018-01-12', currentStartText: '2025-11-17', monthlyRent: '7000', historicalPeriods: history }

test('accepts arbitrary day dates, gaps and changing prices', () => {
  const actual = buildRentSchedule(config)
  assert.equal(actual.stayStartDate, '2018-01-12')
  assert.deepEqual(actual.rentPeriods, [
    { startDate: '2018-01-12', endDate: '2022-07-18', monthlyRent: 3500 },
    { startDate: '2022-08-09', endDate: '2025-10-21', monthlyRent: 5000 },
    { startDate: '2025-11-17', endDate: null, monthlyRent: 7000 },
  ])
})
test('changing retained rent periods preserves original move-in date', () => {
  assert.equal(buildRentSchedule({ ...config, retained: true, stayStart: '2018-01-06' }).stayStartDate, '2018-01-06')
})
test('roundtrips exact day ranges without normalizing months', () => {
  const parsed = scheduleToForm([
    { startDate: '2018-01-12', endDate: '2022-07-18', monthlyRent: 3500 },
    { startDate: '2022-08-09', endDate: '2025-10-21', monthlyRent: 5000 },
    { startDate: '2025-11-17', endDate: null, monthlyRent: 7000 },
  ], '2018-01-12', 7000)
  assert.equal(parsed.currentStartText, '2025-11-17')
  assert.deepEqual(parsed.historicalPeriods, history)
})
test('compatibility: older V7 month fields render as real beginning/end dates', () => {
  const parsed = scheduleToForm([
    { startMonth: '2018-01-01', endMonth: '2022-07-01', monthlyRent: 3500 },
    { startMonth: '2022-08-01', endMonth: null, monthlyRent: 5000 },
  ], '2018-01-12', 5000)
  assert.equal(parsed.historicalPeriods[0].endDate, '2022-07-31')
  assert.equal(parsed.currentStartText, '2022-08-01')
})
test('add next period gives three empty fields, never forces next contiguous month', () => {
  const first = appendHistoricalPeriod([])
  assert.deepEqual(first[0], { startDate: '', endDate: '', monthlyRent: '' })
  const second = appendHistoricalPeriod(history)
  assert.equal(second.length, 3)
  assert.deepEqual(second[2], { startDate: '', endDate: '', monthlyRent: '' })
})
test('valid gaps accepted, overlapping dates rejected', () => {
  assert.equal(validateRentHistory(config), '')
  assert.notEqual(validateRentHistory({ ...config, historicalPeriods: [
    history[0], { ...history[1], startDate: '2022-07-18' }
  ] }), '')
  assert.notEqual(validateRentHistory({ ...config, currentStartText: '2025-10-21' }), '')
  assert.equal(validateRentHistory({ ...config, currentStartText: '2026-01-08' }), '')
})
test('accepts history inserted out of order and sorts chronologically before submit', () => {
  const schedule = buildRentSchedule({ ...config, historicalPeriods: [...history].reverse() })
  assert.equal(schedule.rentPeriods[0].startDate, '2018-01-12')
  assert.equal(schedule.rentPeriods[1].startDate, '2022-08-09')
})
test('rejects inverted and invalid calendar dates', () => {
  assert.notEqual(validateRentHistory({ ...config, historicalPeriods: [{
    startDate: '2022-07-18', endDate: '2022-07-17', monthlyRent: 3500
  }] }), '')
  assert.notEqual(validateRentHistory({ ...config, historicalPeriods: [{
    startDate: '2022-02-31', endDate: '2022-07-17', monthlyRent: 3500
  }] }), '')
  assert.equal(parseDisplayDate('31/02/2022'), null)
  assert.equal(parseDisplayDate('14/08/2022'), '2022-08-14')
  assert.equal(parseDisplayDate('2022-08-14'), '2022-08-14')
  assert.equal(endOfMonthIso('2024-02'), '2024-02-29')
  assert.equal(displayDate('2025-11-17'), '17/11/2025')
})
test('new occupant without past history keeps exact current start date', () => {
  const occupant = emptyOccupant('2026-11-16', 7000)
  occupant.tenantId = '123'
  const sent = toOccupantPayload([occupant])
  assert.equal(occupant.currentStartText, '2026-11-16')
  assert.equal(sent[0].startDate, '2026-11-16')
  assert.equal(sent[0].rentPeriods, undefined)
})
test('retained tenant: no edit never transmits or modifies rent history', () => {
  const prefilled = prefillCurrentOccupants({ status: 'ACTIVE', occupants: [{
    id: 9, tenant: { id: 11 }, startDate: '2022-10-05', endDate: null, monthlyRent: 5000,
    rentPeriods: [{ startDate: '2022-10-05', endDate: null, monthlyRent: 5000 }],
  }] }, 5000, '2026-11-16')
  const sent = toOccupantPayload(prefilled)
  assert.equal(sent[0].startDate, '2022-10-05')
  assert.equal(sent[0].rentPeriods, undefined)
  assert.equal(sent[0].monthlyRent, 5000)
})
test('retained edit preserves original move-in date and does not opt into invoices', () => {
  const prefilled = prefillCurrentOccupants({ status: 'ACTIVE', occupants: [{
    id: 1, tenant: { id: 11 }, startDate: '2018-01-06', endDate: null, monthlyRent: 7000,
    rentPeriods: [
      { startDate: '2018-01-12', endDate: '2022-07-18', monthlyRent: 3500 },
      { startDate: '2022-08-09', endDate: '2025-10-21', monthlyRent: 5000 },
      { startDate: '2025-11-17', endDate: null, monthlyRent: 7000 },
    ],
  }]}, 7000, '2026-11-16')
  const sent = toOccupantPayload([{ ...prefilled[0], editRetainedSchedule: true }])
  assert.equal(sent[0].startDate, '2018-01-06')
  assert.equal(sent[0].rentPeriods.length, 3)
  assert.equal(sent[0].rentPeriods[0].endDate, '2022-07-18')
  assert.equal(sent[0].generateMissingPayments, false)
})
