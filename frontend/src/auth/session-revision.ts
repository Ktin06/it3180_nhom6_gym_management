export interface CookieRevision {
  session: string | null
  refresh: string | null
}

const databaseName = 'gym-auth-cookie'
const storeName = 'coordination'
const revisionKey = 'revision'
let connection: Promise<IDBDatabase> | undefined

function database(): Promise<IDBDatabase> {
  connection ??= new Promise<IDBDatabase>((resolve, reject) => {
    const request = indexedDB.open(databaseName, 1)
    request.onupgradeneeded = () => request.result.createObjectStore(storeName)
    request.onerror = () => reject(request.error)
    request.onsuccess = () => {
      const db = request.result
      db.onclose = db.onversionchange = () => {
        db.close()
        connection = undefined
      }
      resolve(db)
    }
  }).catch((error: unknown) => {
    connection = undefined
    throw error
  })
  return connection
}

export async function readCookieRevision(): Promise<CookieRevision> {
  const db = await database()
  return new Promise((resolve, reject) => {
    const request = db.transaction(storeName).objectStore(storeName).get(revisionKey)
    request.onsuccess = () => resolve(request.result ?? { session: null, refresh: null })
    request.onerror = () => reject(request.error)
  })
}

export async function writeCookieRevision(revision: CookieRevision): Promise<void> {
  const db = await database()
  return new Promise((resolve, reject) => {
    // Tab giữ Web Lock đến khi commit để lượt kế tiếp đọc được đúng lần đổi cookie.
    const transaction = db.transaction(storeName, 'readwrite')
    transaction.oncomplete = () => resolve()
    transaction.onabort = () => reject(transaction.error)
    transaction.objectStore(storeName).put(revision, revisionKey)
  })
}
