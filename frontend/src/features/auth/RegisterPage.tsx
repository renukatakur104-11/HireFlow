import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import toast from 'react-hot-toast';
import { Briefcase, ArrowRight } from 'lucide-react';
import { registerSchema, RegisterFormValues } from '../../lib/validation/authSchemas';
import { useAuth } from '../../contexts/AuthContext';
import apiClient from '../../lib/api/client';
import { API } from '../../lib/api/endpoints';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Card } from '../../components/ui/Card';

const BRANCHES = ['Computer Science', 'Information Technology', 'Electronics & Communication', 'Mechanical Engineering', 'Civil Engineering', 'Chemical Engineering', 'Electrical Engineering', 'Other'];

export function RegisterPage() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [isLoading, setIsLoading] = useState(false);
  const { register, handleSubmit, formState: { errors } } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { skills: [] },
  });

  const onSubmit = async (data: RegisterFormValues) => {
    setIsLoading(true);
    try {
      const res = await apiClient.post(API.auth.register, data);
      const { accessToken, refreshToken, user } = res.data.data;
      login(accessToken, refreshToken, user);
      toast.success('Account created! Complete your profile to start applying.');
      navigate('/profile');
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } };
      toast.error(e.response?.data?.message || 'Registration failed. Please try again.');
    } finally { setIsLoading(false); }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-indigo-950 to-slate-900 flex items-center justify-center p-4">
      <div className="w-full max-w-lg space-y-6">
        <div className="text-center">
          <div className="inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-indigo-600 shadow-xl shadow-indigo-500/30 mb-4">
            <Briefcase className="h-7 w-7 text-white" />
          </div>
          <h1 className="text-3xl font-extrabold text-white">Create your account</h1>
          <p className="mt-1 text-slate-400 text-sm">Start discovering placement opportunities</p>
        </div>
        <Card className="border-slate-700/50 bg-slate-800/50 backdrop-blur-sm">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Input label="Full Name" placeholder="Priya Sharma" error={errors.fullName?.message} {...register('fullName')} />
              <Input label="Email" type="email" placeholder="you@college.edu" error={errors.email?.message} {...register('email')} />
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Input label="Phone" placeholder="9876543210" error={errors.phone?.message} {...register('phone')} />
              <Input label="Password" type="password" placeholder="Min. 8 characters" error={errors.password?.message} {...register('password')} />
            </div>
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">Branch</label>
              <select className="flex h-10 w-full rounded-lg border border-slate-600 bg-slate-700/50 px-3 py-2 text-sm text-white focus:border-indigo-500 focus:outline-none" {...register('branch')}>
                <option value="">Select branch</option>
                {BRANCHES.map(b => <option key={b} value={b}>{b}</option>)}
              </select>
              {errors.branch && <p className="text-xs text-rose-400">{errors.branch.message}</p>}
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Input label="Graduation Year" type="number" placeholder="2026" error={errors.graduationYear?.message} {...register('graduationYear')} />
              <Input label="CGPA (0–10)" type="number" step="0.01" placeholder="8.5" error={errors.cgpa?.message} {...register('cgpa')} />
            </div>
            <Button type="submit" isLoading={isLoading} className="w-full" rightIcon={<ArrowRight className="w-4 h-4" />}>
              Create Account
            </Button>
          </form>
        </Card>
        <p className="text-center text-slate-400 text-sm">
          Already have an account? <Link to="/login" className="text-indigo-400 hover:text-indigo-300 font-medium">Sign in</Link>
        </p>
      </div>
    </div>
  );
}
