import { createClient } from '@supabase/supabase-js'

// DEBUGGING/CLEANUP GUIDE:
// If photos remain in Supabase after tenant deletion, you can manually clean them up:
// 
// 1. Open browser DevTools (F12) and go to Console tab
// 2. Import the functions:
//    - To list all photos: await window.supabaseStorage?.listPhotosInBucket?.()
//    - To delete a specific photo by path: await window.supabaseStorage?.deletePhotoByPath?.('filename-here.jpg')
//
// Note: Check browser console for detailed logs of deletion attempts

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY
const bucket = import.meta.env.VITE_SUPABASE_TENANT_PHOTO_BUCKET || 'tenant-photos'

const supabase = supabaseUrl && supabaseAnonKey ? createClient(supabaseUrl, supabaseAnonKey) : null

export async function uploadTenantPhoto(file) {
  if (!file) return null
  if (!file.type.startsWith('image/')) throw new Error('Please select an image file.')
  if (file.size > 5 * 1024 * 1024) throw new Error('Tenant photos must be 5 MB or smaller.')
  if (!supabase) throw new Error('Supabase storage is not configured.')

  const extension = file.name.split('.').pop()?.toLowerCase() || 'jpg'
  const path = `${crypto.randomUUID()}.${extension}`
  const { error } = await supabase.storage.from(bucket).upload(path, file, {
    cacheControl: '3600',
    contentType: file.type,
    upsert: false,
  })
  if (error) throw error

  return supabase.storage.from(bucket).getPublicUrl(path).data.publicUrl
}

export async function deleteTenantPhoto(photoUrl) {
  if (!photoUrl) return
  if (!supabase) return

  try {
    // Try to parse the photo URL to extract the file path
    const url = new URL(photoUrl)
    const segments = url.pathname.split('/').filter(Boolean)
    
    // Handle both formats: /storage/v1/object/public/bucket-name/file-path or direct file path
    let filePath
    
    // Check if this is a Supabase public URL
    if (url.hostname.includes('supabase') || url.hostname.includes('amazonaws')) {
      const bucketIndex = segments.indexOf(bucket)
      if (bucketIndex === -1 || bucketIndex + 1 >= segments.length) {
        // Try to extract from the end - file paths are usually at the end
        filePath = segments.slice(-1)[0] || null
      } else {
        filePath = segments.slice(bucketIndex + 1).join('/')
      }
    } else {
      // Assume it's just a file name or path
      filePath = segments.join('/') || photoUrl.split('/').pop()
    }

    if (!filePath) {
      console.warn('Could not extract file path from photo URL:', photoUrl)
      return
    }

    console.log('Attempting to delete file:', filePath, 'from bucket:', bucket)
    
    const { error, data } = await supabase.storage.from(bucket).remove([filePath])
    
    if (error) {
      console.error('Error deleting photo from Supabase:', error)
      throw error
    }
    
    console.log('Photo deleted successfully:', filePath)
  } catch (err) {
    // Log the error but don't block tenant deletion
    console.error('Failed to delete tenant photo from Supabase:', err, 'Photo URL:', photoUrl)
  }
}

/**
 * List all photos in the Supabase bucket (for debugging/cleanup purposes)
 */
export async function listPhotosInBucket() {
  if (!supabase) throw new Error('Supabase storage is not configured.')
  
  try {
    const { data, error } = await supabase.storage.from(bucket).list()
    if (error) throw error
    return data || []
  } catch (err) {
    console.error('Failed to list photos in bucket:', err)
    throw err
  }
}

/**
 * Delete a photo by file path (for manual cleanup of orphaned photos)
 */
export async function deletePhotoByPath(filePath) {
  if (!filePath) return
  if (!supabase) throw new Error('Supabase storage is not configured.')
  
  try {
    console.log('Deleting file:', filePath, 'from bucket:', bucket)
    const { error } = await supabase.storage.from(bucket).remove([filePath])
    
    if (error) {
      console.error('Error deleting photo:', error)
      throw error
    }
    
    console.log('Photo deleted successfully:', filePath)
  } catch (err) {
    console.error('Failed to delete photo:', err)
    throw err
  }
}

// Expose utility functions for debugging in development mode
if (import.meta.env.DEV && typeof window !== 'undefined') {
  window.supabaseStorage = {
    listPhotosInBucket,
    deletePhotoByPath,
  }
}