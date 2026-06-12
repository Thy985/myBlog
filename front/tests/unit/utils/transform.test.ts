import { describe, it, expect } from 'vitest'
import { camelToSnake, snakeToCamel, transformKeys } from '@/utils/transform'

describe('camelToSnake - 驼峰转蛇形', () => {
  it('应该将驼峰命名转换为蛇形命名', () => {
    expect(camelToSnake('camelCase')).toBe('camel_case')
  })

  it('应该处理多个大写字母', () => {
    expect(camelToSnake('someVariableName')).toBe('some_variable_name')
  })

  it('应该处理单个单词', () => {
    expect(camelToSnake('name')).toBe('name')
  })

  it('应该处理全大写开头的驼峰', () => {
    expect(camelToSnake('FirstName')).toBe('_first_name')
  })

  it('应该处理空字符串', () => {
    expect(camelToSnake('')).toBe('')
  })

  it('应该处理非字符串输入', () => {
    expect(camelToSnake(null)).toBe('')
  })
})

describe('snakeToCamel - 蛇形转驼峰', () => {
  it('应该将蛇形命名转换为驼峰命名', () => {
    expect(snakeToCamel('snake_case')).toBe('snakeCase')
  })

  it('应该处理多个下划线', () => {
    expect(snakeToCamel('some_variable_name')).toBe('someVariableName')
  })

  it('应该处理没有下划线的字符串', () => {
    expect(snakeToCamel('name')).toBe('name')
  })

  it('应该处理前导下划线', () => {
    expect(snakeToCamel('_private_field')).toBe('PrivateField')
  })

  it('应该处理空字符串', () => {
    expect(snakeToCamel('')).toBe('')
  })

  it('应该处理非字符串输入', () => {
    expect(snakeToCamel(null)).toBe('')
  })
})

describe('transformKeys - 对象键名转换', () => {
  it('应该递归转换对象键名（驼峰转蛇形）', () => {
    const input = {
      firstName: 'John',
      lastName: 'Doe',
      homeAddress: {
        streetName: 'Main St',
        zipCode: '10001'
      }
    }

    const output = transformKeys(input, camelToSnake)

    expect(output).toEqual({
      first_name: 'John',
      last_name: 'Doe',
      home_address: {
        street_name: 'Main St',
        zip_code: '10001'
      }
    })
  })

  it('应该递归转换对象键名（蛇形转驼峰）', () => {
    const input = {
      first_name: 'John',
      last_name: 'Doe',
      home_address: {
        street_name: 'Main St',
        zip_code: '10001'
      }
    }

    const output = transformKeys(input, snakeToCamel)

    expect(output).toEqual({
      firstName: 'John',
      lastName: 'Doe',
      homeAddress: {
        streetName: 'Main St',
        zipCode: '10001'
      }
    })
  })

  it('应该处理嵌套数组中的对象', () => {
    const input = {
      userList: [
        { firstName: 'Alice', lastName: 'Smith' },
        { firstName: 'Bob', lastName: 'Jones' }
      ]
    }

    const output = transformKeys(input, camelToSnake)

    expect(output).toEqual({
      user_list: [
        { first_name: 'Alice', last_name: 'Smith' },
        { first_name: 'Bob', last_name: 'Jones' }
      ]
    })
  })

  it('应该处理 null 值', () => {
    const input = {
      name: null,
      age: 30
    }

    const output = transformKeys(input, camelToSnake)

    expect(output).toEqual({
      name: null,
      age: 30
    })
  })

  it('应该处理 undefined 值', () => {
    const input = {
      name: undefined,
      age: 30
    }

    const output = transformKeys(input, camelToSnake)

    expect(output).toHaveProperty('name')
    expect(output.name).toBe(undefined)
    expect(output.age).toBe(30)
  })

  it('应该处理 null 输入', () => {
    expect(transformKeys(null, camelToSnake)).toBe(null)
  })

  it('应该处理 undefined 输入', () => {
    expect(transformKeys(undefined, snakeToCamel)).toBe(undefined)
  })

  it('应该保留非对象值不变', () => {
    expect(transformKeys('hello', camelToSnake)).toBe('hello')
    expect(transformKeys(42, camelToSnake)).toBe(42)
    expect(transformKeys(true, camelToSnake)).toBe(true)
  })

  it('应该处理深层嵌套的对象和数组', () => {
    const input = {
      companyInfo: {
        companyName: 'Tech Corp',
        departments: [
          {
            departmentName: 'Engineering',
            teamMembers: [
              { firstName: 'Alice', skills: ['JavaScript', 'TypeScript'] }
            ]
          }
        ]
      }
    }

    const output = transformKeys(input, camelToSnake)

    expect(output).toEqual({
      company_info: {
        company_name: 'Tech Corp',
        departments: [
          {
            department_name: 'Engineering',
            team_members: [
              { first_name: 'Alice', skills: ['JavaScript', 'TypeScript'] }
            ]
          }
        ]
      }
    })
  })

  it('应该处理空对象', () => {
    expect(transformKeys({}, camelToSnake)).toEqual({})
  })

  it('应该处理空数组', () => {
    const output = transformKeys({ items: [] }, camelToSnake)
    expect(output).toEqual({ items: [] })
  })
})
