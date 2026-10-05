import { z } from 'zod';

export const profileUpdateSchema = z.object({
  fullName: z.string().min(2, 'Full name must be at least 2 characters').max(150).optional().or(z.literal('')),
  phone: z.string().regex(/^[0-9+()\\-\\s]{10,20}$/, 'Enter a valid phone number').optional().or(z.literal('')),
  branch: z.string().optional(),
  graduationYear: z.coerce.number().min(2020).max(2035).optional(),
  cgpa: z.coerce.number().min(0).max(10).optional(),
  backlogs: z.coerce.number().min(0).optional(),
  skills: z.array(z.string()).optional(),
});

export type ProfileUpdateFormValues = z.infer<typeof profileUpdateSchema>;
