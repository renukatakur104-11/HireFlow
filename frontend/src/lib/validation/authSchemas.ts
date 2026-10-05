import { z } from 'zod';

export const loginSchema = z.object({
  email: z.string().email('Please enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
});

export const registerSchema = z.object({
  email: z.string().email('Please enter a valid email address'),
  password: z.string().min(8, 'Password must be at least 8 characters'),
  fullName: z.string().min(2, 'Full name must be at least 2 characters').max(150),
  phone: z.string().regex(/^[0-9+()\\-\\s]{10,20}$/, 'Enter a valid phone number'),
  branch: z.string().min(1, 'Please select your branch'),
  graduationYear: z.coerce.number().min(2020, 'Enter a valid graduation year').max(2035),
  cgpa: z.coerce.number().min(0).max(10, 'CGPA must be between 0 and 10'),
  skills: z.array(z.string()).optional().default([]),
});

export type LoginFormValues = z.infer<typeof loginSchema>;
export type RegisterFormValues = z.infer<typeof registerSchema>;
