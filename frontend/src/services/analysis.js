import { listTransactions } from './transactions.js'

export async function loadAnalysisData(signal) {
  const transactions = await listTransactions(signal)
  if (!Array.isArray(transactions)) throw new Error('The server returned an invalid transaction list.')
  return transactions
}
